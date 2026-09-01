package com.coffeeshop.repository;

import com.coffeeshop.dto.CheckoutCommands;
import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.CheckoutModels;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Atomic SQL Server checkout: payments, inventory sale, order completion, and table release. */
public final class JdbcCheckoutRepository implements CheckoutRepository {
    private record Method(long id, String code) { }
    private record Stock(long id, BigDecimal current, byte[] rowVersion) { }

    private final DataSource dataSource;

    public JdbcCheckoutRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override public CheckoutModels.Context loadContext(long orderId, long userId) {
        String orderSql = "SELECT order_number,total_amount,status,row_version FROM dbo.orders WHERE order_id=?";
        String methodsSql = "SELECT payment_method_id,method_code,method_name FROM dbo.payment_methods WHERE is_active=1 ORDER BY display_order,method_name";
        String shiftSql = """
            SELECT rs.register_shift_id,r.register_name
            FROM dbo.register_shifts rs
            JOIN dbo.registers r ON r.register_id=rs.register_id
            WHERE rs.opened_by_user_id=? AND rs.status='OPEN'
            """;
        try (Connection connection = dataSource.getConnection()) {
            String number;
            BigDecimal total;
            byte[] rowVersion;
            try (PreparedStatement statement = connection.prepareStatement(orderSql)) {
                statement.setLong(1, orderId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) throw new IllegalArgumentException("Order was not found.");
                    if (!"OPEN".equals(results.getString("status")))
                        throw new IllegalStateException("Only open orders can be checked out.");
                    number = results.getString("order_number");
                    total = results.getBigDecimal("total_amount");
                    rowVersion = results.getBytes("row_version");
                }
            }
            List<CheckoutModels.PaymentMethod> methods = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(methodsSql);
                 ResultSet results = statement.executeQuery()) {
                while (results.next()) methods.add(new CheckoutModels.PaymentMethod(
                        results.getLong(1), results.getString(2), results.getString(3)));
            }
            if (methods.isEmpty()) throw new IllegalStateException("No active payment methods are available.");
            Long shiftId = null;
            String registerName = null;
            try (PreparedStatement statement = connection.prepareStatement(shiftSql)) {
                statement.setLong(1, userId);
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        shiftId = results.getLong(1);
                        registerName = results.getString(2);
                    }
                }
            }
            return new CheckoutModels.Context(orderId, number, total, rowVersion,
                    methods, shiftId, registerName);
        } catch (RuntimeException error) {
            throw error;
        } catch (SQLException error) {
            throw failure("Unable to prepare order checkout.", error);
        }
    }

    @Override public void complete(CheckoutCommands.Checkout checkout) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                Long tableId = lockOrder(connection, checkout);
                ensureNoPaymentsOrSale(connection, checkout.orderId());
                Map<Long, Method> methods = lockPaymentMethods(connection, checkout.payments());
                Long shiftId = lockCashShiftIfRequired(connection, checkout.userId(), checkout.payments(), methods);
                Map<Long, BigDecimal> requirements = loadRequirements(connection, checkout.orderId());
                List<Stock> stocks = lockAndValidateStock(connection, requirements);

                insertPayments(connection, checkout, methods, shiftId);
                consumeStock(connection, checkout, requirements, stocks);
                completeOrder(connection, checkout);
                releaseTable(connection, tableId);
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw failure("Unable to complete the order.", error);
        }
    }

    private static Long lockOrder(Connection connection, CheckoutCommands.Checkout checkout) throws SQLException {
        String sql = """
            SELECT o.status,o.total_amount,o.row_version,d.cafe_table_id
            FROM dbo.orders o WITH(UPDLOCK,HOLDLOCK)
            LEFT JOIN dbo.dine_in_orders d ON d.order_id=o.order_id
            WHERE o.order_id=?
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, checkout.orderId());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) throw new IllegalArgumentException("Order was not found.");
                if (!"OPEN".equals(results.getString(1)))
                    throw new IllegalStateException("This order is no longer open.");
                if (results.getBigDecimal(2).compareTo(checkout.expectedTotal()) != 0
                        || !Arrays.equals(results.getBytes(3), checkout.orderRowVersion()))
                    throw new IllegalStateException("The order changed. Refresh and try again.");
                long value = results.getLong(4);
                return results.wasNull() ? null : value;
            }
        }
    }

    private static void ensureNoPaymentsOrSale(Connection connection, long orderId) throws SQLException {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM dbo.payments WITH(UPDLOCK,HOLDLOCK) WHERE order_id=?),
              (SELECT COUNT(*) FROM dbo.inventory_movements WITH(UPDLOCK,HOLDLOCK)
                 WHERE order_id=? AND movement_type='SALE')
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, orderId);
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                if (results.getInt(1) != 0 || results.getInt(2) != 0)
                    throw new IllegalStateException("This order already has checkout activity. Refresh and review it.");
            }
        }
    }

    private static Map<Long, Method> lockPaymentMethods(Connection connection,
                                                         List<CheckoutCommands.Payment> payments) throws SQLException {
        Set<Long> ids = new LinkedHashSet<>();
        payments.forEach(payment -> ids.add(payment.paymentMethodId()));
        Map<Long, Method> methods = new HashMap<>();
        String sql = "SELECT payment_method_id,method_code FROM dbo.payment_methods WITH(UPDLOCK,HOLDLOCK) WHERE payment_method_id=? AND is_active=1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (long id : ids.stream().sorted().toList()) {
                statement.setLong(1, id);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) throw new IllegalStateException("A selected payment method is no longer active.");
                    methods.put(id, new Method(id, results.getString(2)));
                }
            }
        }
        return methods;
    }

    private static Long lockCashShiftIfRequired(Connection connection, long userId,
                                                 List<CheckoutCommands.Payment> payments,
                                                 Map<Long, Method> methods) throws SQLException {
        boolean cash = payments.stream().anyMatch(payment ->
                "CASH".equals(methods.get(payment.paymentMethodId()).code()));
        if (!cash) return null;
        String sql = "SELECT register_shift_id FROM dbo.register_shifts WITH(UPDLOCK,HOLDLOCK) WHERE opened_by_user_id=? AND status='OPEN'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next())
                    throw new IllegalStateException("Open a register shift before accepting cash.");
                return results.getLong(1);
            }
        }
    }

    private static Map<Long, BigDecimal> loadRequirements(Connection connection, long orderId) throws SQLException {
        String sql = """
            SELECT usage.inventory_item_id,
                   CAST(ROUND(SUM(usage.required_quantity),4) AS DECIMAL(19,4)) required_quantity
            FROM (
                SELECT ri.inventory_item_id,
                       CAST(ri.quantity_required * oi.quantity AS DECIMAL(38,8)) required_quantity
                FROM dbo.order_items oi
                JOIN dbo.recipe_items ri ON ri.product_variant_id=oi.product_variant_id
                WHERE oi.order_id=?
                UNION ALL
                SELECT mri.inventory_item_id,
                       CAST(mri.quantity_required * oim.quantity AS DECIMAL(38,8)) required_quantity
                FROM dbo.order_items oi
                JOIN dbo.order_item_modifiers oim ON oim.order_item_id=oi.order_item_id
                JOIN dbo.modifier_recipe_items mri ON mri.modifier_id=oim.modifier_id
                WHERE oi.order_id=?
            ) usage
            GROUP BY usage.inventory_item_id
            ORDER BY usage.inventory_item_id
            """;
        Map<Long, BigDecimal> requirements = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, orderId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    BigDecimal quantity = results.getBigDecimal(2);
                    if (quantity.signum() > 0) requirements.put(results.getLong(1), quantity);
                }
            }
        }
        return requirements;
    }

    private static List<Stock> lockAndValidateStock(Connection connection,
                                                     Map<Long, BigDecimal> requirements) throws SQLException {
        String sql = "SELECT current_stock_quantity,allow_negative_stock,row_version,item_name FROM dbo.inventory_items WITH(UPDLOCK,HOLDLOCK) WHERE inventory_item_id=?";
        List<Stock> stocks = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<Long, BigDecimal> requirement : requirements.entrySet()) {
                statement.setLong(1, requirement.getKey());
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) throw new IllegalStateException("A recipe inventory item no longer exists.");
                    BigDecimal current = results.getBigDecimal(1);
                    boolean allowNegative = results.getBoolean(2);
                    BigDecimal remaining = current.subtract(requirement.getValue());
                    if (!allowNegative && remaining.signum() < 0)
                        throw new IllegalStateException("Insufficient stock for " + results.getString(4) + ".");
                    stocks.add(new Stock(requirement.getKey(), current, results.getBytes(3)));
                }
            }
        }
        return stocks;
    }

    private static void insertPayments(Connection connection, CheckoutCommands.Checkout checkout,
                                       Map<Long, Method> methods, Long cashShiftId) throws SQLException {
        String sql = "INSERT dbo.payments(order_id,payment_method_id,register_shift_id,recorded_by_user_id,amount,status,external_reference) VALUES(?,?,?,?,?,'COMPLETED',?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (CheckoutCommands.Payment payment : checkout.payments()) {
                Method method = methods.get(payment.paymentMethodId());
                statement.setLong(1, checkout.orderId());
                statement.setLong(2, payment.paymentMethodId());
                if ("CASH".equals(method.code())) statement.setLong(3, cashShiftId);
                else statement.setNull(3, Types.BIGINT);
                statement.setLong(4, checkout.userId());
                statement.setBigDecimal(5, payment.amount());
                if (payment.externalReference() == null) statement.setNull(6, Types.NVARCHAR);
                else statement.setString(6, payment.externalReference());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void consumeStock(Connection connection, CheckoutCommands.Checkout checkout,
                                     Map<Long, BigDecimal> requirements, List<Stock> stocks) throws SQLException {
        String movementSql = "INSERT dbo.inventory_movements(inventory_item_id,order_id,recorded_by_user_id,movement_type,quantity_delta) VALUES(?,?,?,'SALE',?)";
        String updateSql = "UPDATE dbo.inventory_items SET current_stock_quantity=?,updated_at=SYSUTCDATETIME() WHERE inventory_item_id=? AND row_version=?";
        try (PreparedStatement movement = connection.prepareStatement(movementSql);
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            for (Stock stock : stocks) {
                BigDecimal required = requirements.get(stock.id());
                BigDecimal remaining = stock.current().subtract(required).setScale(4);
                movement.setLong(1, stock.id());
                movement.setLong(2, checkout.orderId());
                movement.setLong(3, checkout.userId());
                movement.setBigDecimal(4, required.negate());
                movement.executeUpdate();
                update.setBigDecimal(1, remaining);
                update.setLong(2, stock.id());
                update.setBytes(3, stock.rowVersion());
                if (update.executeUpdate() != 1)
                    throw new IllegalStateException("Inventory changed during checkout. Refresh and try again.");
            }
        }
    }

    private static void completeOrder(Connection connection, CheckoutCommands.Checkout checkout) throws SQLException {
        String sql = "UPDATE dbo.orders SET status='COMPLETED',completed_at=SYSUTCDATETIME(),updated_at=SYSUTCDATETIME() WHERE order_id=? AND status='OPEN' AND row_version=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, checkout.orderId());
            statement.setBytes(2, checkout.orderRowVersion());
            if (statement.executeUpdate() != 1)
                throw new IllegalStateException("The order changed during checkout. Refresh and try again.");
        }
    }

    private static void releaseTable(Connection connection, Long tableId) throws SQLException {
        if (tableId == null) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE dbo.cafe_tables SET status='AVAILABLE',updated_at=SYSUTCDATETIME() WHERE cafe_table_id=? AND status='OCCUPIED'")) {
            statement.setLong(1, tableId);
            statement.executeUpdate();
        }
    }

    private static DatabaseException failure(String message, Exception cause) {
        return new DatabaseException(message, cause);
    }
}
