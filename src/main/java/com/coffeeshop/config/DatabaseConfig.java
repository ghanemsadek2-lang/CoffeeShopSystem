package com.coffeeshop.config;

import java.util.Objects;

/** Validated database and connection-pool configuration. */
public final class DatabaseConfig {

    private final String jdbcUrl;
    private final DatabaseAuthentication authentication;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeoutMs;
    private final long validationTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;

    DatabaseConfig(
            String jdbcUrl,
            DatabaseAuthentication authentication,
            String username,
            String password,
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeoutMs,
            long validationTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs
    ) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.authentication = Objects.requireNonNull(authentication);
        this.username = username;
        this.password = password;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.validationTimeoutMs = validationTimeoutMs;
        this.idleTimeoutMs = idleTimeoutMs;
        this.maxLifetimeMs = maxLifetimeMs;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public DatabaseAuthentication authentication() {
        return authentication;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public int maximumPoolSize() {
        return maximumPoolSize;
    }

    public int minimumIdle() {
        return minimumIdle;
    }

    public long connectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public long validationTimeoutMs() {
        return validationTimeoutMs;
    }

    public long idleTimeoutMs() {
        return idleTimeoutMs;
    }

    public long maxLifetimeMs() {
        return maxLifetimeMs;
    }

    @Override
    public String toString() {
        return "DatabaseConfig{authentication=" + authentication
                + ", maximumPoolSize=" + maximumPoolSize
                + ", minimumIdle=" + minimumIdle + '}';
    }
}
