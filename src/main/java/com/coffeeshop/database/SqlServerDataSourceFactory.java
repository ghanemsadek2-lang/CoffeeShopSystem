package com.coffeeshop.database;

import com.coffeeshop.config.DatabaseAuthentication;
import com.coffeeshop.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Creates the bounded SQL Server connection pool used by the desktop application. */
public final class SqlServerDataSourceFactory {

    private static final String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private SqlServerDataSourceFactory() {
    }

    public static HikariDataSource create(DatabaseConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("CoffeeShopPool");
        hikari.setDriverClassName(SQL_SERVER_DRIVER);
        hikari.setJdbcUrl(config.jdbcUrl());
        if (config.authentication() == DatabaseAuthentication.SQL_SERVER) {
            hikari.setUsername(config.username());
            hikari.setPassword(config.password());
        }
        hikari.setMaximumPoolSize(config.maximumPoolSize());
        hikari.setMinimumIdle(config.minimumIdle());
        hikari.setConnectionTimeout(config.connectionTimeoutMs());
        hikari.setValidationTimeout(config.validationTimeoutMs());
        hikari.setIdleTimeout(config.idleTimeoutMs());
        hikari.setMaxLifetime(config.maxLifetimeMs());
        hikari.setAutoCommit(true);
        hikari.setInitializationFailTimeout(config.connectionTimeoutMs());
        return new HikariDataSource(hikari);
    }
}
