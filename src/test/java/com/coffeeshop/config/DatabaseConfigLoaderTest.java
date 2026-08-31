package com.coffeeshop.config;

import com.coffeeshop.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseConfigLoaderTest {

    private final DatabaseConfigLoader loader = new DatabaseConfigLoader();

    @Test
    void loadsSqlServerAuthenticationWithSafePoolDefaults() {
        Properties properties = baseSqlServerProperties();

        DatabaseConfig config = loader.load(properties, Map.of());

        assertEquals(DatabaseAuthentication.SQL_SERVER, config.authentication());
        assertEquals("local_test_user", config.username());
        assertEquals(5, config.maximumPoolSize());
        assertEquals(1, config.minimumIdle());
        assertEquals(10_000, config.connectionTimeoutMs());
        assertFalse(config.toString().contains("local_test_password"));
    }

    @Test
    void environmentOverridesFileWithoutIncludingSecretInDiagnostics() {
        Properties properties = baseSqlServerProperties();
        Map<String, String> environment = new HashMap<>();
        environment.put("COFFEESHOP_DATABASE_USERNAME", "environment_user");
        environment.put("COFFEESHOP_DATABASE_PASSWORD", "environment_secret");
        environment.put("COFFEESHOP_DATABASE_POOL_MAXIMUM_SIZE", "7");

        DatabaseConfig config = loader.load(properties, environment);

        assertEquals("environment_user", config.username());
        assertEquals("environment_secret", config.password());
        assertEquals(7, config.maximumPoolSize());
        assertFalse(config.toString().contains("environment_secret"));
    }

    @Test
    void preservesPasswordCharactersExactly() {
        Properties properties = baseSqlServerProperties();
        Map<String, String> environment = Map.of(
                "COFFEESHOP_DATABASE_PASSWORD", "  exact secret value  "
        );

        DatabaseConfig config = loader.load(properties, environment);

        assertEquals("  exact secret value  ", config.password());
        assertFalse(config.toString().contains("exact secret value"));
    }

    @Test
    void windowsAuthenticationDoesNotRequireUsernameOrPassword() {
        Properties properties = new Properties();
        properties.setProperty("database.url",
                "jdbc:sqlserver://localhost:1433;databaseName=CoffeeShopDev;integratedSecurity=true");
        properties.setProperty("database.authentication", "WINDOWS");

        DatabaseConfig config = loader.load(properties, Map.of());

        assertEquals(DatabaseAuthentication.WINDOWS, config.authentication());
        assertNull(config.username());
        assertNull(config.password());
    }

    @Test
    void rejectsMissingPasswordWithoutExposingOtherCredentialValues() {
        Properties properties = baseSqlServerProperties();
        properties.remove("database.password");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> loader.load(properties, Map.of())
        );

        assertEquals(
                "Required database setting database.password is missing. "
                        + "Set it in the external configuration file or with COFFEESHOP_DATABASE_PASSWORD.",
                exception.getMessage()
        );
        assertFalse(exception.getMessage().contains("local_test_user"));
    }

    @Test
    void rejectsCredentialsEmbeddedInJdbcUrl() {
        Properties properties = baseSqlServerProperties();
        properties.setProperty("database.url",
                "jdbc:sqlserver://localhost:1433;databaseName=CoffeeShopDev;user=sensitive-value");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> loader.load(properties, Map.of())
        );

        assertEquals(
                "Credentials must not be embedded in database.url; use dedicated external settings.",
                exception.getMessage()
        );
        assertFalse(exception.getMessage().contains("sensitive-value"));
    }

    @Test
    void rejectsInvalidPoolRelationship() {
        Properties properties = baseSqlServerProperties();
        properties.setProperty("database.pool.maximum-size", "2");
        properties.setProperty("database.pool.minimum-idle", "3");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> loader.load(properties, Map.of())
        );

        assertEquals(
                "database.pool.minimum-idle must be between 0 and database.pool.maximum-size.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsPoolIntegerOutsideJavaIntegerRange() {
        Properties properties = baseSqlServerProperties();
        properties.setProperty("database.pool.maximum-size", "-2147483649");

        ConfigurationException exception = assertThrows(
                ConfigurationException.class,
                () -> loader.load(properties, Map.of())
        );

        assertEquals(
                "Database setting database.pool.maximum-size is outside the valid range.",
                exception.getMessage()
        );
    }

    private static Properties baseSqlServerProperties() {
        Properties properties = new Properties();
        properties.setProperty("database.url",
                "jdbc:sqlserver://localhost:1433;databaseName=CoffeeShopDev;encrypt=true");
        properties.setProperty("database.authentication", "SQL_SERVER");
        properties.setProperty("database.username", "local_test_user");
        properties.setProperty("database.password", "local_test_password");
        return properties;
    }
}
