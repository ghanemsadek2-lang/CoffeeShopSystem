package com.coffeeshop.database;

import com.coffeeshop.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Objects;

/** Owns the application connection pool and closes it during application shutdown. */
public final class DatabaseConnectionManager implements AutoCloseable {

    private final HikariDataSource dataSource;

    private DatabaseConnectionManager(HikariDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public static DatabaseConnectionManager open(DatabaseConfig config) {
        return new DatabaseConnectionManager(SqlServerDataSourceFactory.create(config));
    }

    public DataSource dataSource() {
        if (dataSource.isClosed()) {
            throw new IllegalStateException("The database connection pool is closed.");
        }
        return dataSource;
    }

    public boolean isClosed() {
        return dataSource.isClosed();
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
