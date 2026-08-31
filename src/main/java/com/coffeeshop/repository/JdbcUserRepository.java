package com.coffeeshop.repository;

import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.security.UserCredentials;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** SQL Server-backed authentication repository using bounded, parameterized queries. */
public final class JdbcUserRepository implements UserRepository {

    private static final String FIND_ACTIVE_USER = """
            SELECT u.user_id,
                   u.employee_id,
                   u.username,
                   u.password_hash,
                   u.must_change_password,
                   u.locked_until,
                   e.first_name,
                   e.last_name
            FROM dbo.users AS u
            LEFT JOIN dbo.employees AS e ON e.employee_id = u.employee_id
            WHERE u.username = ?
              AND u.is_active = 1
              AND (u.employee_id IS NULL OR e.is_active = 1)
            """;

    private static final String FIND_ACTIVE_ROLES = """
            SELECT r.role_id, r.role_code, r.role_name
            FROM dbo.user_roles AS ur
            INNER JOIN dbo.roles AS r ON r.role_id = ur.role_id
            WHERE ur.user_id = ?
              AND r.is_active = 1
            ORDER BY r.role_code
            """;

    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<UserCredentials> findActiveByUsername(String username) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_USER)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                Long employeeId = nullableLong(result, "employee_id");
                String storedUsername = result.getString("username");
                return Optional.of(new UserCredentials(
                        result.getLong("user_id"),
                        employeeId,
                        storedUsername,
                        displayName(result, storedUsername),
                        result.getString("password_hash"),
                        result.getBoolean("must_change_password"),
                        nullableInstant(result, "locked_until")
                ));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to complete the authentication database lookup.", exception);
        }
    }

    @Override
    public Set<RoleInfo> findActiveRoles(long userId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ACTIVE_ROLES)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                Set<RoleInfo> roles = new LinkedHashSet<>();
                while (result.next()) {
                    roles.add(new RoleInfo(
                            result.getLong("role_id"),
                            result.getString("role_code"),
                            result.getString("role_name")
                    ));
                }
                return Set.copyOf(roles);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load authorization roles.", exception);
        }
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static Instant nullableInstant(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String displayName(ResultSet result, String fallback) throws SQLException {
        String firstName = result.getString("first_name");
        String lastName = result.getString("last_name");
        if (firstName == null || lastName == null) {
            return fallback;
        }
        return firstName + ' ' + lastName;
    }
}
