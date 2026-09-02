package com.coffeeshop.repository;

import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.*;
import javax.sql.DataSource;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

/** Read models and transactional cancellation for operational order/table management. */
public final class JdbcOrderManagementRepository implements OrderManagementRepository {
    private static final String ORDER_SELECT = """
        SELECT o.order_id,o.order_number,os.source_code,o.order_type,o.status,o.opened_at,o.total_amount,t.table_name
        FROM dbo.orders o
        JOIN dbo.order_sources os ON os.order_source_id=o.order_source_id
        LEFT JOIN dbo.dine_in_orders dio ON dio.order_id=o.order_id
        LEFT JOIN dbo.cafe_tables t ON t.cafe_table_id=dio.cafe_table_id
        """;
    private final DataSource dataSource;
    public JdbcOrderManagementRepository(DataSource dataSource) { this.dataSource = Objects.requireNonNull(dataSource); }

    public List<OrderSummary> findOrders(OrderStatus status) {
        String sql = ORDER_SELECT + " WHERE (? IS NULL OR o.status=?) ORDER BY o.opened_at DESC";
        try (Connection connection=dataSource.getConnection(); PreparedStatement statement=connection.prepareStatement(sql)) {
            if (status == null) { statement.setNull(1,Types.VARCHAR); statement.setNull(2,Types.VARCHAR); }
            else { statement.setString(1,status.name()); statement.setString(2,status.name()); }
            try (ResultSet results=statement.executeQuery()) {
                List<OrderSummary> orders=new ArrayList<>(); while(results.next()) orders.add(map(results)); return List.copyOf(orders);
            }
        } catch (SQLException error) { throw failure("Unable to load orders.",error); }
    }

    public Optional<OrderDetails> findDetails(long orderId) {
        String head=ORDER_SELECT + " WHERE o.order_id=?";
        String items="""
            SELECT oi.order_item_id,oi.product_name_snapshot,oi.variant_name_snapshot,oi.quantity,oi.total_amount,
                   STRING_AGG(oim.modifier_name_snapshot, ', ') WITHIN GROUP (ORDER BY oim.order_item_modifier_id) AS modifiers
            FROM dbo.order_items oi
            LEFT JOIN dbo.order_item_modifiers oim ON oim.order_item_id=oi.order_item_id
            WHERE oi.order_id=?
            GROUP BY oi.order_item_id,oi.product_name_snapshot,oi.variant_name_snapshot,oi.quantity,oi.total_amount
            ORDER BY oi.order_item_id
            """;
        String payments="""
            SELECT COUNT(*),COALESCE(SUM(CASE WHEN status IN ('COMPLETED','PARTIALLY_REFUNDED','REFUNDED') THEN amount-refunded_amount ELSE 0 END),0)
            FROM dbo.payments WHERE order_id=?
            """;
        String discounts="""
            SELECT application_sequence,discount_name_snapshot,applied_discount_amount
            FROM dbo.order_discounts WHERE order_id=? ORDER BY application_sequence
            """;
        try (Connection connection=dataSource.getConnection(); PreparedStatement statement=connection.prepareStatement(head)) {
            statement.setLong(1,orderId);
            try (ResultSet results=statement.executeQuery()) {
                if (!results.next()) return Optional.empty();
                OrderSummary summary=map(results); List<String> lines=new ArrayList<>();
                try (PreparedStatement lineStatement=connection.prepareStatement(items)) {
                    lineStatement.setLong(1,orderId);
                    try (ResultSet itemResults=lineStatement.executeQuery()) {
                        while(itemResults.next()) {
                            String modifiers=itemResults.getString(6);
                            lines.add(itemResults.getBigDecimal(4).stripTrailingZeros().toPlainString()+" x "+itemResults.getString(2)+" - "+itemResults.getString(3)
                                    +(modifiers==null?"":" ["+modifiers+"]")+" - "+itemResults.getBigDecimal(5).setScale(2,RoundingMode.HALF_UP));
                        }
                    }
                }
                try (PreparedStatement paymentStatement=connection.prepareStatement(payments)) {
                    paymentStatement.setLong(1,orderId);
                    try (ResultSet paymentResults=paymentStatement.executeQuery()) {
                        paymentResults.next(); int count=paymentResults.getInt(1);
                        lines.add(count==0 ? "Payments: none recorded" : "Payments: "+count+" - net "+paymentResults.getBigDecimal(2).setScale(2,RoundingMode.HALF_UP));
                    }
                }
                try (PreparedStatement discountStatement=connection.prepareStatement(discounts)) {
                    discountStatement.setLong(1,orderId);
                    try (ResultSet discountResults=discountStatement.executeQuery()) {
                        while(discountResults.next()) lines.add("Discount "+discountResults.getInt(1)+": "
                                +discountResults.getString(2)+" - "+discountResults.getBigDecimal(3).setScale(2,RoundingMode.HALF_UP));
                    }
                }
                return Optional.of(new OrderDetails(summary,List.copyOf(lines)));
            }
        } catch (SQLException error) { throw failure("Unable to load order details.",error); }
    }

    public void cancelOpenOrder(long orderId) {
        try (Connection connection=dataSource.getConnection()) {
            connection.setAutoCommit(false); connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                Long tableId=null;
                try (PreparedStatement query=connection.prepareStatement("SELECT o.status,d.cafe_table_id FROM dbo.orders o WITH(UPDLOCK,HOLDLOCK) LEFT JOIN dbo.dine_in_orders d ON d.order_id=o.order_id WHERE o.order_id=?")) {
                    query.setLong(1,orderId); try(ResultSet results=query.executeQuery()) {
                        if(!results.next()) throw new IllegalArgumentException("Order was not found.");
                        if(!"OPEN".equals(results.getString(1))) throw new IllegalStateException("Only open orders can be cancelled.");
                        long value=results.getLong(2); if(!results.wasNull()) tableId=value;
                    }
                }
                try (PreparedStatement update=connection.prepareStatement("UPDATE dbo.orders SET status='CANCELLED',cancelled_at=SYSUTCDATETIME(),updated_at=SYSUTCDATETIME() WHERE order_id=?")) { update.setLong(1,orderId); update.executeUpdate(); }
                if(tableId!=null) try (PreparedStatement update=connection.prepareStatement("UPDATE dbo.cafe_tables SET status='AVAILABLE',updated_at=SYSUTCDATETIME() WHERE cafe_table_id=? AND status='OCCUPIED'")) { update.setLong(1,tableId); update.executeUpdate(); }
                connection.commit();
            } catch(Exception error) { connection.rollback(); throw error; }
        } catch(RuntimeException error) { throw error; }
        catch(Exception error) { throw failure("Unable to cancel the order.",error); }
    }

    public List<CafeTableInfo> findTables() {
        String sql="SELECT cafe_table_id,table_code,table_name,capacity,status FROM dbo.cafe_tables WHERE is_active=1 ORDER BY display_order,table_name";
        try(Connection connection=dataSource.getConnection();PreparedStatement statement=connection.prepareStatement(sql);ResultSet results=statement.executeQuery()) {
            List<CafeTableInfo> tables=new ArrayList<>(); while(results.next()) tables.add(new CafeTableInfo(results.getLong(1),results.getString(2),results.getString(3),results.getInt(4),results.getString(5))); return List.copyOf(tables);
        } catch(SQLException error) { throw failure("Unable to load cafe tables.",error); }
    }

    private static OrderSummary map(ResultSet results)throws SQLException {
        return new OrderSummary(results.getLong(1),results.getString(2),results.getString(3),OrderType.valueOf(results.getString(4)),OrderStatus.valueOf(results.getString(5)),results.getTimestamp(6).toLocalDateTime(),results.getBigDecimal(7),results.getString(8));
    }
    private static DatabaseException failure(String message,Exception error) { return new DatabaseException(message,error); }
}
