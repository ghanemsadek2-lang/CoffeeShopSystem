package com.coffeeshop.config;

import com.coffeeshop.exception.ConfigurationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Loads and validates external database configuration without logging secrets. */
public final class DatabaseConfigLoader {

    public static final String CONFIG_PATH_PROPERTY = "coffeeshop.config.path";
    public static final String CONFIG_PATH_ENVIRONMENT = "COFFEESHOP_CONFIG_PATH";

    private static final Path DEFAULT_CONFIG_PATH = Path.of("config", "application.properties");

    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 5;
    private static final int DEFAULT_MINIMUM_IDLE = 1;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 10_000;
    private static final long DEFAULT_VALIDATION_TIMEOUT_MS = 5_000;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 300_000;
    private static final long DEFAULT_MAX_LIFETIME_MS = 1_800_000;

    public DatabaseConfig load() {
        return load(resolveConfigPath(), System.getenv());
    }

    public DatabaseConfig load(Path path) {
        return load(path, System.getenv());
    }

    DatabaseConfig load(Path path, Map<String, String> environment) {
        if (!Files.isRegularFile(path)) {
            throw new ConfigurationException(
                    "Database configuration file was not found at " + path.toAbsolutePath()
                            + ". Copy application.example.properties to config/application.properties "
                            + "or set " + CONFIG_PATH_PROPERTY + "/" + CONFIG_PATH_ENVIRONMENT + "."
            );
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new ConfigurationException(
                    "Database configuration could not be read from " + path.toAbsolutePath() + '.',
                    exception
            );
        }
        return load(properties, environment);
    }

    DatabaseConfig load(Properties properties, Map<String, String> environment) {
        String url = required(properties, environment, "database.url", "COFFEESHOP_DATABASE_URL");
        validateJdbcUrl(url);

        String authenticationValue = required(
                properties,
                environment,
                "database.authentication",
                "COFFEESHOP_DATABASE_AUTHENTICATION"
        );
        DatabaseAuthentication authentication = parseAuthentication(authenticationValue);

        String username = optional(properties, environment, "database.username", "COFFEESHOP_DATABASE_USERNAME");
        String password = secret(properties, environment, "database.password", "COFFEESHOP_DATABASE_PASSWORD");
        if (authentication == DatabaseAuthentication.SQL_SERVER) {
            requirePresent(username, "database.username", "COFFEESHOP_DATABASE_USERNAME");
            requirePresent(password, "database.password", "COFFEESHOP_DATABASE_PASSWORD");
        }

        int maximumPoolSize = integer(properties, environment, "database.pool.maximum-size",
                "COFFEESHOP_DATABASE_POOL_MAXIMUM_SIZE", DEFAULT_MAXIMUM_POOL_SIZE);
        int minimumIdle = integer(properties, environment, "database.pool.minimum-idle",
                "COFFEESHOP_DATABASE_POOL_MINIMUM_IDLE", DEFAULT_MINIMUM_IDLE);
        long connectionTimeoutMs = longValue(properties, environment, "database.pool.connection-timeout-ms",
                "COFFEESHOP_DATABASE_POOL_CONNECTION_TIMEOUT_MS", DEFAULT_CONNECTION_TIMEOUT_MS);
        long validationTimeoutMs = longValue(properties, environment, "database.pool.validation-timeout-ms",
                "COFFEESHOP_DATABASE_POOL_VALIDATION_TIMEOUT_MS", DEFAULT_VALIDATION_TIMEOUT_MS);
        long idleTimeoutMs = longValue(properties, environment, "database.pool.idle-timeout-ms",
                "COFFEESHOP_DATABASE_POOL_IDLE_TIMEOUT_MS", DEFAULT_IDLE_TIMEOUT_MS);
        long maxLifetimeMs = longValue(properties, environment, "database.pool.max-lifetime-ms",
                "COFFEESHOP_DATABASE_POOL_MAX_LIFETIME_MS", DEFAULT_MAX_LIFETIME_MS);

        validatePoolSettings(maximumPoolSize, minimumIdle, connectionTimeoutMs,
                validationTimeoutMs, idleTimeoutMs, maxLifetimeMs);

        return new DatabaseConfig(url, authentication, username, password, maximumPoolSize, minimumIdle,
                connectionTimeoutMs, validationTimeoutMs, idleTimeoutMs, maxLifetimeMs);
    }

    private Path resolveConfigPath() {
        String systemPath = trimToNull(System.getProperty(CONFIG_PATH_PROPERTY));
        if (systemPath != null) {
            return Path.of(systemPath);
        }
        String environmentPath = trimToNull(System.getenv(CONFIG_PATH_ENVIRONMENT));
        return environmentPath == null ? DEFAULT_CONFIG_PATH : Path.of(environmentPath);
    }

    private static String required(Properties properties, Map<String, String> environment,
                                   String propertyName, String environmentName) {
        String value = optional(properties, environment, propertyName, environmentName);
        requirePresent(value, propertyName, environmentName);
        return value;
    }

    private static void requirePresent(String value, String propertyName, String environmentName) {
        if (value == null) {
            throw new ConfigurationException(
                    "Required database setting " + propertyName + " is missing. "
                            + "Set it in the external configuration file or with " + environmentName + '.'
            );
        }
    }

    private static String optional(Properties properties, Map<String, String> environment,
                                   String propertyName, String environmentName) {
        String environmentValue = trimToNull(environment.get(environmentName));
        return environmentValue != null ? environmentValue : trimToNull(properties.getProperty(propertyName));
    }

    private static int integer(Properties properties, Map<String, String> environment,
                               String propertyName, String environmentName, int defaultValue) {
        long value = longValue(properties, environment, propertyName, environmentName, defaultValue);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ConfigurationException("Database setting " + propertyName + " is outside the valid range.");
        }
        return (int) value;
    }

    private static String secret(Properties properties, Map<String, String> environment,
                                 String propertyName, String environmentName) {
        String environmentValue = environment.get(environmentName);
        if (environmentValue != null && !environmentValue.isEmpty()) {
            return environmentValue;
        }
        String propertyValue = properties.getProperty(propertyName);
        return propertyValue == null || propertyValue.isEmpty() ? null : propertyValue;
    }

    private static long longValue(Properties properties, Map<String, String> environment,
                                  String propertyName, String environmentName, long defaultValue) {
        String value = optional(properties, environment, propertyName, environmentName);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new ConfigurationException("Database setting " + propertyName + " must be an integer.");
        }
    }

    private static DatabaseAuthentication parseAuthentication(String value) {
        try {
            return DatabaseAuthentication.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigurationException(
                    "database.authentication must be SQL_SERVER or WINDOWS."
            );
        }
    }

    private static void validateJdbcUrl(String url) {
        String normalized = url.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("jdbc:sqlserver://")) {
            throw new ConfigurationException("database.url must be a SQL Server JDBC URL.");
        }
        if (!normalized.contains(";databasename=")) {
            throw new ConfigurationException("database.url must explicitly include databaseName.");
        }
        if (normalized.contains(";password=") || normalized.contains(";user=")
                || normalized.contains(";username=")) {
            throw new ConfigurationException(
                    "Credentials must not be embedded in database.url; use dedicated external settings."
            );
        }
    }

    private static void validatePoolSettings(int maximumPoolSize, int minimumIdle,
                                             long connectionTimeoutMs, long validationTimeoutMs,
                                             long idleTimeoutMs, long maxLifetimeMs) {
        if (maximumPoolSize < 1 || maximumPoolSize > 20) {
            throw new ConfigurationException("database.pool.maximum-size must be between 1 and 20.");
        }
        if (minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new ConfigurationException(
                    "database.pool.minimum-idle must be between 0 and database.pool.maximum-size."
            );
        }
        if (connectionTimeoutMs < 250) {
            throw new ConfigurationException("database.pool.connection-timeout-ms must be at least 250.");
        }
        if (validationTimeoutMs < 250 || validationTimeoutMs > connectionTimeoutMs) {
            throw new ConfigurationException(
                    "database.pool.validation-timeout-ms must be at least 250 and no greater than the connection timeout."
            );
        }
        if (idleTimeoutMs < 10_000) {
            throw new ConfigurationException("database.pool.idle-timeout-ms must be at least 10000.");
        }
        if (maxLifetimeMs < 30_000) {
            throw new ConfigurationException("database.pool.max-lifetime-ms must be at least 30000.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
