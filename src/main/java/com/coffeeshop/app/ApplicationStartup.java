package com.coffeeshop.app;

import com.coffeeshop.config.DatabaseConfig;
import com.coffeeshop.config.DatabaseConfigLoader;
import com.coffeeshop.database.DatabaseConnectionManager;
import com.coffeeshop.database.DatabaseHealthCheck;
import com.coffeeshop.database.FlywayMigrationService;
import com.coffeeshop.repository.JdbcUserRepository;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.security.BcryptPasswordVerifier;
import com.coffeeshop.service.AuthenticationService;

/** Performs ordered, fail-fast initialization before any business UI is shown. */
public final class ApplicationStartup {

    private static final String USER_MESSAGE =
            "Database initialization failed. Verify the local configuration and database availability, then try again.";

    public ApplicationContext initialize() {
        DatabaseConnectionManager connectionManager = null;
        try {
            DatabaseConfig config = new DatabaseConfigLoader().load();
            connectionManager = DatabaseConnectionManager.open(config);
            new DatabaseHealthCheck(connectionManager.dataSource()).verify();
            new FlywayMigrationService(connectionManager.dataSource()).validate();

            JdbcUserRepository userRepository = new JdbcUserRepository(connectionManager.dataSource());
            AuthenticationService authenticationService = new AuthenticationService(
                    userRepository,
                    new BcryptPasswordVerifier()
            );
            return new ApplicationContext(
                    connectionManager,
                    authenticationService,
                    new ApplicationSession()
            );
        } catch (RuntimeException exception) {
            if (connectionManager != null) {
                connectionManager.close();
            }
            throw new StartupException(USER_MESSAGE, exception);
        }
    }
}
