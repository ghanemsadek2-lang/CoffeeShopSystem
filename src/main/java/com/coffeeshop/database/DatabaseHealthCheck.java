package com.coffeeshop.database;

import com.coffeeshop.exception.DatabaseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Performs a read-only connectivity check without touching application data. */
public final class DatabaseHealthCheck {

    private static final String HEALTH_QUERY = "SELECT 1";

    private final DataSource dataSource;

    public DatabaseHealthCheck(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public void verify() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(HEALTH_QUERY);
             ResultSet result = statement.executeQuery()) {
            if (!result.next() || result.getInt(1) != 1) {
                throw new DatabaseException("The database health check returned an unexpected result.");
            }
        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to verify the database connection. Check the external database configuration and server availability.",
                    exception
            );
        }
    }
}
