package com.coffeeshop.repository;

import com.coffeeshop.dto.OrderDiscountCommands;
import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.OrderDiscountModels;
import com.coffeeshop.service.OrderDiscountService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public final class JdbcOrderDiscountRepository implements OrderDiscountRepository {
    private record OrderState(String number, BigDecimal subtotal, BigDecimal discount,
                              BigDecimal tax, BigDecimal total, byte[] version) {}
    private record DiscountState(long id, String code, String name, String type, BigDecimal value) {}
    private record Line(long id, BigDecimal subtotal, BigDecimal discount,
                        BigDecimal taxRate, byte[] version) {
        BigDecimal remaining() { return subtotal.subtract(discount); }
    }
    private final DataSource dataSource;

    public JdbcOrderDiscountRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override public OrderDiscountModels.Context prepare(long orderId) {
        try (Connection connection = dataSource.getConnection()) {
            OrderState order = order(connection, orderId, false);
            List<OrderDiscountModels.DiscountOption> available = available(connection, orderId);
            List<OrderDiscountModels.AppliedDiscount> applied = applied(connection, orderId);
            return new OrderDiscountModels.Context(orderId, order.number(), order.subtotal(), order.discount(),
                    order.tax(), order.total(), order.version(), available, applied);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw failure("Unable to load order discounts.", exception);
        }
    }

    @Override public void apply(OrderDiscountCommands.Apply command) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                OrderState order = order(connection, command.orderId(), true);
                if (!Arrays.equals(order.version(), command.orderRowVersion()))
                    throw new IllegalStateException("The order changed. Refresh and try again.");
                DiscountState discount = discount(connection, command.discountId());
                int sequence = nextSequence(connection, command.orderId(), command.discountId());
                List<Line> lines = lines(connection, command.orderId());
                BigDecimal remainingBase = lines.stream().map(Line::remaining)
                        .reduce(zero(), BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
                if (remainingBase.compareTo(order.subtotal().subtract(order.discount())) != 0)
                    throw new IllegalStateException("Order totals are inconsistent. Refresh and review the order.");
                BigDecimal amount = OrderDiscountService.calculate(discount.type(), discount.value(), remainingBase);
                List<BigDecimal> allocations = OrderDiscountService.allocate(
                        lines.stream().map(Line::remaining).toList(), amount);
                Totals totals = updateLines(connection, lines, allocations);
                insertSnapshot(connection, command.orderId(), discount, sequence, amount);
                updateOrder(connection, command, totals);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failure("Unable to apply the discount.", exception);
        }
    }

    private static OrderState order(Connection connection, long orderId, boolean lock) throws SQLException {
        String hint = lock ? " WITH(UPDLOCK,HOLDLOCK)" : "";
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT order_number,subtotal_amount,discount_amount,tax_amount,total_amount,status,row_version " +
                        "FROM dbo.orders" + hint + " WHERE order_id=?")) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Order was not found.");
                if (!"OPEN".equals(result.getString(6)))
                    throw new IllegalStateException("Discounts may only be applied to open orders.");
                return new OrderState(result.getString(1), result.getBigDecimal(2), result.getBigDecimal(3),
                        result.getBigDecimal(4), result.getBigDecimal(5), result.getBytes(7));
            }
        }
    }

    private static List<OrderDiscountModels.DiscountOption> available(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT d.discount_id,d.discount_code,d.discount_name,d.discount_type,d.discount_value," +
                "d.valid_from,d.valid_until FROM dbo.discounts d WHERE d.is_active=1 " +
                "AND (d.valid_from IS NULL OR d.valid_from<=SYSUTCDATETIME()) " +
                "AND (d.valid_until IS NULL OR SYSUTCDATETIME()<d.valid_until) " +
                "AND NOT EXISTS(SELECT 1 FROM dbo.order_discounts od WHERE od.order_id=? AND od.discount_id=d.discount_id) " +
                "ORDER BY d.discount_name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderDiscountModels.DiscountOption> values = new ArrayList<>();
                while (result.next()) {
                    Timestamp from = result.getTimestamp(6), until = result.getTimestamp(7);
                    values.add(new OrderDiscountModels.DiscountOption(result.getLong(1), result.getString(2),
                            result.getString(3), result.getString(4), result.getBigDecimal(5),
                            from == null ? null : from.toLocalDateTime(), until == null ? null : until.toLocalDateTime()));
                }
                return values;
            }
        }
    }

    private static List<OrderDiscountModels.AppliedDiscount> applied(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT application_sequence,discount_code_snapshot,discount_name_snapshot," +
                "discount_type_snapshot,discount_value_snapshot,applied_discount_amount " +
                "FROM dbo.order_discounts WHERE order_id=? ORDER BY application_sequence";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderDiscountModels.AppliedDiscount> values = new ArrayList<>();
                while (result.next()) values.add(new OrderDiscountModels.AppliedDiscount(result.getInt(1),
                        result.getString(2), result.getString(3), result.getString(4),
                        result.getBigDecimal(5), result.getBigDecimal(6)));
                return values;
            }
        }
    }

    private static DiscountState discount(Connection connection, long discountId) throws SQLException {
        String sql = "SELECT discount_id,discount_code,discount_name,discount_type,discount_value " +
                "FROM dbo.discounts WITH(UPDLOCK,HOLDLOCK) WHERE discount_id=? AND is_active=1 " +
                "AND (valid_from IS NULL OR valid_from<=SYSUTCDATETIME()) " +
                "AND (valid_until IS NULL OR SYSUTCDATETIME()<valid_until)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, discountId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("The selected discount is no longer available.");
                return new DiscountState(result.getLong(1), result.getString(2), result.getString(3),
                        result.getString(4), result.getBigDecimal(5));
            }
        }
    }

    private static int nextSequence(Connection connection, long orderId, long discountId) throws SQLException {
        try (PreparedStatement duplicate = connection.prepareStatement(
                "SELECT 1 FROM dbo.order_discounts WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? AND discount_id=?")) {
            duplicate.setLong(1, orderId); duplicate.setLong(2, discountId);
            try (ResultSet result = duplicate.executeQuery()) {
                if (result.next()) throw new IllegalStateException("This discount is already applied to the order.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(application_sequence),0)+1 FROM dbo.order_discounts WITH(UPDLOCK,HOLDLOCK) WHERE order_id=?")) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); }
        }
    }

    private static List<Line> lines(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT order_item_id,subtotal_amount,discount_amount,tax_rate_percent,row_version " +
                "FROM dbo.order_items WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY subtotal_amount-discount_amount,order_item_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                List<Line> values = new ArrayList<>();
                while (result.next()) values.add(new Line(result.getLong(1), result.getBigDecimal(2),
                        result.getBigDecimal(3), result.getBigDecimal(4), result.getBytes(5)));
                if (values.isEmpty()) throw new IllegalStateException("The order has no items to discount.");
                return values;
            }
        }
    }

    private record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total) {}
    private static Totals updateLines(Connection connection, List<Line> lines,
                                      List<BigDecimal> allocations) throws SQLException {
        BigDecimal subtotal = zero(), discount = zero(), tax = zero(), total = zero();
        String sql = "UPDATE dbo.order_items SET discount_amount=?,tax_amount=?,total_amount=?," +
                "updated_at=SYSUTCDATETIME() WHERE order_item_id=? AND row_version=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < lines.size(); index++) {
                Line line = lines.get(index);
                BigDecimal lineDiscount = line.discount().add(allocations.get(index)).setScale(4);
                BigDecimal lineTax = line.subtotal().subtract(lineDiscount).multiply(line.taxRate())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal lineTotal = line.subtotal().subtract(lineDiscount).add(lineTax).setScale(4);
                statement.setBigDecimal(1, lineDiscount); statement.setBigDecimal(2, lineTax);
                statement.setBigDecimal(3, lineTotal); statement.setLong(4, line.id()); statement.setBytes(5, line.version());
                if (statement.executeUpdate() != 1)
                    throw new IllegalStateException("An order item changed. Refresh and try again.");
                subtotal = subtotal.add(line.subtotal()); discount = discount.add(lineDiscount);
                tax = tax.add(lineTax); total = total.add(lineTotal);
            }
        }
        return new Totals(subtotal.setScale(4), discount.setScale(4), tax.setScale(4), total.setScale(4));
    }

    private static void insertSnapshot(Connection connection, long orderId, DiscountState discount,
                                       int sequence, BigDecimal amount) throws SQLException {
        String sql = "INSERT dbo.order_discounts(order_id,discount_id,application_sequence,discount_code_snapshot," +
                "discount_name_snapshot,discount_type_snapshot,discount_value_snapshot,applied_discount_amount) " +
                "VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId); statement.setLong(2, discount.id()); statement.setInt(3, sequence);
            statement.setString(4, discount.code()); statement.setString(5, discount.name());
            statement.setString(6, discount.type()); statement.setBigDecimal(7, discount.value());
            statement.setBigDecimal(8, amount); statement.executeUpdate();
        }
    }

    private static void updateOrder(Connection connection, OrderDiscountCommands.Apply command,
                                    Totals totals) throws SQLException {
        String sql = "UPDATE dbo.orders SET subtotal_amount=?,discount_amount=?,tax_amount=?,total_amount=?," +
                "updated_at=SYSUTCDATETIME() WHERE order_id=? AND status='OPEN' AND row_version=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, totals.subtotal()); statement.setBigDecimal(2, totals.discount());
            statement.setBigDecimal(3, totals.tax()); statement.setBigDecimal(4, totals.total());
            statement.setLong(5, command.orderId()); statement.setBytes(6, command.orderRowVersion());
            if (statement.executeUpdate() != 1)
                throw new IllegalStateException("The order changed. Refresh and try again.");
        }
    }

    private static BigDecimal zero() { return BigDecimal.ZERO.setScale(4); }
    private static DatabaseException failure(String message, Exception exception) {
        return new DatabaseException(message, exception);
    }
}
