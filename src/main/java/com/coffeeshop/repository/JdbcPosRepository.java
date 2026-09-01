package com.coffeeshop.repository;
import com.coffeeshop.dto.CreateOrderRequest;
import com.coffeeshop.dto.DeliveryInfo;
import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.*;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Transactional SQL Server persistence for menu browsing and open-order creation. */
public final class JdbcPosRepository implements PosRepository {
    private final DataSource dataSource;
    public JdbcPosRepository(DataSource dataSource){this.dataSource=Objects.requireNonNull(dataSource);}

    public List<MenuCategory> findActiveCategories(){
        String sql="SELECT category_id,category_name FROM dbo.categories WHERE is_active=1 ORDER BY display_order,category_name";
        try(Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement(sql); ResultSet r=s.executeQuery()){
            List<MenuCategory> out=new ArrayList<>(); while(r.next())out.add(new MenuCategory(r.getLong(1),r.getString(2))); return List.copyOf(out);
        }catch(SQLException e){throw failure("Unable to load menu categories.",e);}
    }
    public List<MenuVariant> findActiveMenuVariants(){
        String sql="""
            SELECT v.product_variant_id,p.product_id,p.category_id,p.product_name,v.variant_name,v.selling_price,
                   COALESCE(v.preparation_station_id,p.preparation_station_id) station_id,s.station_code,s.station_name
            FROM dbo.product_variants v JOIN dbo.products p ON p.product_id=v.product_id
            LEFT JOIN dbo.preparation_stations s ON s.preparation_station_id=COALESCE(v.preparation_station_id,p.preparation_station_id)
            WHERE v.is_active=1 AND p.is_active=1 ORDER BY p.display_order,p.product_name,v.display_order,v.variant_name
            """;
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){
            List<MenuVariant> out=new ArrayList<>();while(r.next()){long sid=r.getLong("station_id");boolean noStation=r.wasNull();out.add(new MenuVariant(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getBigDecimal(6),noStation?null:sid,r.getString(8),r.getString(9)));}return List.copyOf(out);
        }catch(SQLException e){throw failure("Unable to load menu products.",e);}
    }
    public List<PosModifierGroup> findActiveModifierGroups(long productId){
        try(Connection c=dataSource.getConnection()){return modifierGroups(c,productId,false);}
        catch(SQLException e){throw failure("Unable to load product choices.",e);}
    }
    private List<PosModifierGroup> modifierGroups(Connection c,long productId,boolean lock)throws SQLException{
        String hint=lock?" WITH(UPDLOCK,HOLDLOCK)":"";
        String sql="""
            SELECT a.product_modifier_group_assignment_id,g.modifier_group_id,g.group_name,
                   COALESCE(a.minimum_selections_override,g.minimum_selections) effective_minimum,
                   COALESCE(a.maximum_selections_override,g.maximum_selections) effective_maximum,
                   m.modifier_id,m.modifier_name,m.price_adjustment
            FROM dbo.product_modifier_group_assignments a%s
            JOIN dbo.modifier_groups g%s ON g.modifier_group_id=a.modifier_group_id AND g.is_active=1
            LEFT JOIN dbo.modifiers m%s ON m.modifier_group_id=g.modifier_group_id AND m.is_active=1
            WHERE a.product_id=? AND a.is_active=1
            ORDER BY a.display_order,g.group_name,m.display_order,m.modifier_name
            """.formatted(hint,hint,hint);
        try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,productId);try(ResultSet r=s.executeQuery()){
            Map<Long,GroupBuilder> groups=new LinkedHashMap<>();while(r.next()){long assignmentId=r.getLong(1);GroupBuilder group=groups.get(assignmentId);if(group==null){group=new GroupBuilder(assignmentId,r.getLong(2),r.getString(3),r.getInt(4),r.getInt(5));groups.put(assignmentId,group);}long modifierId=r.getLong(6);if(!r.wasNull())group.options.add(new PosModifierOption(modifierId,group.groupId,r.getString(7),r.getBigDecimal(8)));}return groups.values().stream().map(GroupBuilder::build).toList();
        }}
    }
    private static final class GroupBuilder{private final long assignmentId,groupId;private final String name;private final int minimum,maximum;private final List<PosModifierOption> options=new ArrayList<>();private GroupBuilder(long assignmentId,long groupId,String name,int minimum,int maximum){this.assignmentId=assignmentId;this.groupId=groupId;this.name=name;this.minimum=minimum;this.maximum=maximum;}private PosModifierGroup build(){return new PosModifierGroup(assignmentId,groupId,name,minimum,maximum,options);}}
    public List<CafeTableInfo> findAvailableTables(){return findTables("WHERE is_active=1 AND status='AVAILABLE'");}
    private List<CafeTableInfo> findTables(String where){
        String sql="SELECT cafe_table_id,table_code,table_name,capacity,status FROM dbo.cafe_tables "+where+" ORDER BY display_order,table_name";
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){
            List<CafeTableInfo> out=new ArrayList<>();while(r.next())out.add(new CafeTableInfo(r.getLong(1),r.getString(2),r.getString(3),r.getInt(4),r.getString(5)));return List.copyOf(out);
        }catch(SQLException e){throw failure("Unable to load cafe tables.",e);}
    }
    public String createOpenOrder(CreateOrderRequest request){
        try(Connection c=dataSource.getConnection()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{String number=nextOrderNumber(c);long sourceId=sourceId(c);BigDecimal subtotal=request.lines().stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(4);
                long orderId=insertOrder(c,request,number,sourceId,subtotal);
                if(request.orderType()==OrderType.DINE_IN)insertDineIn(c,orderId,request.cafeTableId());
                if(request.orderType()==OrderType.DELIVERY)insertDelivery(c,orderId,request.deliveryInfo());
                for(CartLine line:request.lines())insertLine(c,orderId,line);
                c.commit();return number;
            }catch(Exception e){c.rollback();throw e;}
        }catch(Exception e){if(e instanceof DatabaseException d)return rethrow(d);throw failure("Unable to save the open order.",e);}
    }
    private static <T>T rethrow(DatabaseException e){throw e;}
    private String nextOrderNumber(Connection c)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("SELECT setting_value FROM dbo.settings WHERE setting_key='numbering.order.prefix'");ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Order prefix missing");String prefix=r.getString(1);try(PreparedStatement n=c.prepareStatement("SELECT NEXT VALUE FOR dbo.seq_order_number");ResultSet nr=n.executeQuery()){nr.next();return prefix+String.format("%08d",nr.getLong(1));}}
    }
    private long sourceId(Connection c)throws SQLException{try(PreparedStatement s=c.prepareStatement("SELECT order_source_id FROM dbo.order_sources WHERE source_code='POS' AND is_active=1");ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("POS source missing");return r.getLong(1);}}
    private long insertOrder(Connection c,CreateOrderRequest q,String number,long source,BigDecimal total)throws SQLException{
        String sql="INSERT dbo.orders(order_number,order_source_id,created_by_user_id,order_type,status,subtotal_amount,discount_amount,tax_amount,total_amount) VALUES(?,?,?,?,'OPEN',?,0,0,?)";
        try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setString(1,number);s.setLong(2,source);s.setLong(3,q.userId());s.setString(4,q.orderType().name());s.setBigDecimal(5,total);s.setBigDecimal(6,total);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Order id missing");return r.getLong(1);}}
    }
    private void insertDineIn(Connection c,long orderId,Long tableId)throws SQLException{
        try(PreparedStatement lock=c.prepareStatement("SELECT status FROM dbo.cafe_tables WITH(UPDLOCK,HOLDLOCK) WHERE cafe_table_id=? AND is_active=1")){lock.setLong(1,tableId);try(ResultSet r=lock.executeQuery()){if(!r.next()||!"AVAILABLE".equals(r.getString(1)))throw new DatabaseException("The selected table is no longer available.");}}
        try(PreparedStatement s=c.prepareStatement("INSERT dbo.dine_in_orders(order_id,cafe_table_id) VALUES(?,?)")){s.setLong(1,orderId);s.setLong(2,tableId);s.executeUpdate();}
        try(PreparedStatement s=c.prepareStatement("UPDATE dbo.cafe_tables SET status='OCCUPIED',updated_at=SYSUTCDATETIME() WHERE cafe_table_id=?")){s.setLong(1,tableId);s.executeUpdate();}
    }
    private void insertDelivery(Connection c,long id,DeliveryInfo d)throws SQLException{try(PreparedStatement s=c.prepareStatement("INSERT dbo.delivery_orders(order_id,recipient_name,recipient_phone,address_line1,city) VALUES(?,?,?,?,?)")){s.setLong(1,id);s.setString(2,d.recipientName());s.setString(3,d.phone());s.setString(4,d.addressLine1());s.setString(5,d.city());s.executeUpdate();}}
    private void insertLine(Connection c,long orderId,CartLine line)throws SQLException{
        MenuVariant v=line.variant();BigDecimal qty=BigDecimal.valueOf(line.quantity()).setScale(4);BigDecimal subtotal=line.subtotal();
        String validation="""
            SELECT v.selling_price,
                   p.product_id
            FROM dbo.product_variants v WITH(UPDLOCK,HOLDLOCK)
            JOIN dbo.products p WITH(UPDLOCK,HOLDLOCK) ON p.product_id=v.product_id
            WHERE v.product_variant_id=? AND v.is_active=1 AND p.is_active=1
            """;
        try(PreparedStatement check=c.prepareStatement(validation)){
            check.setLong(1,v.variantId());
            try(ResultSet result=check.executeQuery()){
                if(!result.next())throw new DatabaseException("A selected menu item is no longer available.");
                if(result.getBigDecimal(1).compareTo(v.price())!=0)throw new DatabaseException("A menu price changed. Refresh the menu and try again.");
            }
        }
        validateModifiers(c,v.productId(),line.modifiers());
        String sql="INSERT dbo.order_items(order_id,product_variant_id,preparation_station_id,product_name_snapshot,variant_name_snapshot,preparation_station_code,preparation_station_name,quantity,unit_price,subtotal_amount,discount_amount,tax_rate_percent,tax_amount,total_amount) VALUES(?,?,?,?,?,?,?,?,?,?,0,0,0,?)";
        long orderItemId;try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){s.setLong(1,orderId);s.setLong(2,v.variantId());if(v.stationId()==null){s.setNull(3,Types.BIGINT);s.setNull(6,Types.VARCHAR);s.setNull(7,Types.NVARCHAR);}else{s.setLong(3,v.stationId());s.setString(6,v.stationCode());s.setString(7,v.stationName());}s.setString(4,v.productName());s.setString(5,v.variantName());s.setBigDecimal(8,qty);s.setBigDecimal(9,line.unitPrice());s.setBigDecimal(10,subtotal);s.setBigDecimal(11,subtotal);s.executeUpdate();try(ResultSet keys=s.getGeneratedKeys()){if(!keys.next())throw new SQLException("Order item id missing");orderItemId=keys.getLong(1);}}
        String modifierSql="INSERT dbo.order_item_modifiers(order_item_id,modifier_id,modifier_name_snapshot,quantity,unit_price_adjustment,total_price_adjustment) VALUES(?,?,?,?,?,?)";
        try(PreparedStatement s=c.prepareStatement(modifierSql)){for(SelectedModifier modifier:line.modifiers()){s.setLong(1,orderItemId);s.setLong(2,modifier.modifierId());s.setString(3,modifier.name());s.setBigDecimal(4,qty);s.setBigDecimal(5,modifier.priceAdjustment());s.setBigDecimal(6,modifier.priceAdjustment().multiply(qty).setScale(4));s.addBatch();}if(!line.modifiers().isEmpty())s.executeBatch();}
    }
    private void validateModifiers(Connection c,long productId,List<SelectedModifier> selected)throws SQLException{
        List<PosModifierGroup> groups=modifierGroups(c,productId,true);Map<Long,SelectedModifier> selections=new HashMap<>();for(SelectedModifier modifier:selected)if(selections.put(modifier.modifierId(),modifier)!=null)throw new DatabaseException("A modifier was selected more than once.");Set<Long> allowed=new HashSet<>();for(PosModifierGroup group:groups){int count=0;for(PosModifierOption option:group.options()){allowed.add(option.modifierId());SelectedModifier choice=selections.get(option.modifierId());if(choice!=null){count++;if(choice.groupId()!=group.groupId()||!choice.name().equals(option.name())||choice.priceAdjustment().compareTo(option.priceAdjustment())!=0)throw new DatabaseException("A modifier changed. Choose the item options again.");}}if(count<group.minimumSelections()||count>group.maximumSelections())throw new DatabaseException("The modifier selection is no longer valid. Choose the item options again.");}if(!allowed.containsAll(selections.keySet()))throw new DatabaseException("A selected modifier is no longer available.");
    }
    private static DatabaseException failure(String message,Exception e){return new DatabaseException(message,e);}
}
