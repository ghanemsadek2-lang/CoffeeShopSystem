package com.coffeeshop.app;

import com.coffeeshop.database.DatabaseConnectionManager;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.service.AuthenticationService;

import java.util.Objects;

/** Owns initialized application services and their infrastructure lifecycle. */
public final class ApplicationContext implements AutoCloseable {

    private final DatabaseConnectionManager connectionManager;
    private final AuthenticationService authenticationService;
    private final ApplicationSession session;

    ApplicationContext(DatabaseConnectionManager connectionManager,
                       AuthenticationService authenticationService,
                       ApplicationSession session) {
        this.connectionManager = Objects.requireNonNull(connectionManager);
        this.authenticationService = Objects.requireNonNull(authenticationService);
        this.session = Objects.requireNonNull(session);
    }

    public AuthenticationService authenticationService() {
        return authenticationService;
    }

    public ApplicationSession session() {
        return session;
    }

    @Override
    public void close() {
        session.clear();
        connectionManager.close();
    }
}
