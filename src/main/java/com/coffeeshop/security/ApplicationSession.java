package com.coffeeshop.security;

import com.coffeeshop.model.AuthenticatedUser;

import java.util.Objects;
import java.util.Optional;

/** Thread-safe in-memory holder for the single active desktop user session. */
public final class ApplicationSession {

    private AuthenticatedUser currentUser;

    public synchronized void establish(AuthenticatedUser user) {
        Objects.requireNonNull(user, "Authenticated user is required.");
        if (currentUser != null) {
            throw new IllegalStateException("An authenticated session is already active.");
        }
        currentUser = user;
    }

    public synchronized Optional<AuthenticatedUser> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    public synchronized void clear() {
        currentUser = null;
    }
}
