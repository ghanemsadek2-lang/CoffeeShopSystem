package com.coffeeshop.repository;

import com.coffeeshop.dto.LoyaltyCommands;
import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.LoyaltyModels;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public final class JdbcLoyaltyRepository implements LoyaltyRepository {
    private final DataSource dataSource;

    public JdbcLoyaltyRepository(DataSource dataSource) { this.dataSource = Objects.requireNonNull(dataSource); }

    @Override public LoyaltyModels.Catalog load() {
        try (Connection connection = dataSource.getConnection()) {
            return new LoyaltyModels.Catalog(accounts(connection), customers(connection), transactions(connection));
        } catch (SQLException exception) {
            throw failure("Unable to load loyalty accounts.", exception);
        }
    }

    private static List<LoyaltyModels.Account> accounts(Connection connection) throws SQLException {
        String sql = "SELECT a.loyalty_account_id,a.customer_id,a.membership_number,c.customer_number," +
                "LTRIM(RTRIM(c.first_name+' '+COALESCE(c.last_name,''))),a.points_balance," +
                "a.lifetime_points_earned,a.status,a.enrolled_at,a.row_version " +
                "FROM dbo.customer_loyalty_accounts a JOIN dbo.customers c ON c.customer_id=a.customer_id " +
                "ORDER BY c.first_name,c.last_name,a.membership_number";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            List<LoyaltyModels.Account> values = new ArrayList<>();
            while (result.next()) values.add(new LoyaltyModels.Account(result.getLong(1), result.getLong(2),
                    result.getString(3), result.getString(4), result.getString(5), result.getLong(6),
                    result.getLong(7), result.getString(8), result.getTimestamp(9).toLocalDateTime(), result.getBytes(10)));
            return values;
        }
    }

    private static List<LoyaltyModels.CustomerOption> customers(Connection connection) throws SQLException {
        String sql = "SELECT c.customer_id,c.customer_number,LTRIM(RTRIM(c.first_name+' '+COALESCE(c.last_name,''))) " +
                "FROM dbo.customers c WHERE c.is_active=1 AND NOT EXISTS(SELECT 1 FROM dbo.customer_loyalty_accounts a " +
                "WHERE a.customer_id=c.customer_id) ORDER BY c.first_name,c.last_name";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            List<LoyaltyModels.CustomerOption> values = new ArrayList<>();
            while (result.next()) values.add(new LoyaltyModels.CustomerOption(result.getLong(1), result.getString(2), result.getString(3)));
            return values;
        }
    }

    private static List<LoyaltyModels.Transaction> transactions(Connection connection) throws SQLException {
        String sql = "SELECT t.loyalty_transaction_id,t.loyalty_account_id,a.membership_number," +
                "LTRIM(RTRIM(c.first_name+' '+COALESCE(c.last_name,''))),t.transaction_type,t.points_delta," +
                "u.username,t.reason,t.notes,t.occurred_at FROM dbo.loyalty_transactions t " +
                "JOIN dbo.customer_loyalty_accounts a ON a.loyalty_account_id=t.loyalty_account_id " +
                "JOIN dbo.customers c ON c.customer_id=a.customer_id LEFT JOIN dbo.users u ON u.user_id=t.recorded_by_user_id " +
                "ORDER BY t.occurred_at DESC,t.loyalty_transaction_id DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            List<LoyaltyModels.Transaction> values = new ArrayList<>();
            while (result.next()) values.add(new LoyaltyModels.Transaction(result.getLong(1), result.getLong(2),
                    result.getString(3), result.getString(4), result.getString(5), result.getLong(6),
                    result.getString(7), result.getString(8), result.getString(9), result.getTimestamp(10).toLocalDateTime()));
            return values;
        }
    }

    @Override public long enroll(LoyaltyCommands.Enroll command) {
        String sql = "INSERT dbo.customer_loyalty_accounts(customer_id,membership_number) VALUES(?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.customerId());
            statement.setString(2, command.membershipNumber());
            statement.executeUpdate();
            return generatedKey(statement);
        } catch (SQLException exception) {
            throw failure("Unable to enroll the customer. The customer or membership number may already be enrolled.", exception);
        }
    }

    @Override public long changePoints(LoyaltyCommands.ChangePoints command) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                requireEnabled(connection);
                AccountState state = lockAccount(connection, command.loyaltyAccountId());
                if (!Arrays.equals(state.version(), command.accountVersion()))
                    throw new IllegalStateException("Loyalty account changed. Refresh and try again.");
                if (!"ACTIVE".equals(state.status())) throw new IllegalStateException("The loyalty account is not active.");
                long nextBalance;
                long nextLifetime;
                try {
                    nextBalance = Math.addExact(state.balance(), command.pointsDelta());
                    nextLifetime = "EARN".equals(command.transactionType())
                            ? Math.addExact(state.lifetime(), command.pointsDelta()) : state.lifetime();
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("The points value is too large.");
                }
                if (nextBalance < 0) throw new IllegalArgumentException("Redeemed points exceed the available balance.");
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.customer_loyalty_accounts SET points_balance=?,lifetime_points_earned=?," +
                                "updated_at=SYSUTCDATETIME() WHERE loyalty_account_id=? AND row_version=?")) {
                    statement.setLong(1, nextBalance);
                    statement.setLong(2, nextLifetime);
                    statement.setLong(3, command.loyaltyAccountId());
                    statement.setBytes(4, command.accountVersion());
                    if (statement.executeUpdate() != 1)
                        throw new IllegalStateException("Loyalty account changed. Refresh and try again.");
                }
                long transactionId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT dbo.loyalty_transactions(loyalty_account_id,recorded_by_user_id,transaction_type," +
                                "points_delta,reason,notes) VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, command.loyaltyAccountId());
                    statement.setLong(2, command.recordedByUserId());
                    statement.setString(3, command.transactionType());
                    statement.setLong(4, command.pointsDelta());
                    nullable(statement, 5, command.reason());
                    nullable(statement, 6, command.notes());
                    statement.executeUpdate();
                    transactionId = generatedKey(statement);
                }
                connection.commit();
                return transactionId;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failure("Unable to update loyalty points.", exception);
        }
    }

    private static AccountState lockAccount(Connection connection, long accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT points_balance,lifetime_points_earned,status,row_version FROM dbo.customer_loyalty_accounts " +
                        "WITH(UPDLOCK,HOLDLOCK) WHERE loyalty_account_id=?")) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Loyalty account no longer exists.");
                return new AccountState(result.getLong(1), result.getLong(2), result.getString(3), result.getBytes(4));
            }
        }
    }

    private static void requireEnabled(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT setting_value FROM dbo.settings WHERE setting_key='loyalty.enabled'" );
             ResultSet result = statement.executeQuery()) {
            if (result.next() && !Boolean.parseBoolean(result.getString(1)))
                throw new IllegalStateException("The loyalty program is disabled in Settings.");
        }
    }

    private record AccountState(long balance, long lifetime, String status, byte[] version) {}
    private static void nullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) statement.setNull(index, Types.NVARCHAR); else statement.setString(index, value);
    }
    private static long generatedKey(PreparedStatement statement) throws SQLException {
        try (ResultSet result = statement.getGeneratedKeys()) {
            if (!result.next()) throw new SQLException("Generated key unavailable.");
            return result.getLong(1);
        }
    }
    private static DatabaseException failure(String message, Exception exception) {
        return new DatabaseException(message, exception);
    }
}
