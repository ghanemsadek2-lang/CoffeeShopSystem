package com.coffeeshop.service;

import com.coffeeshop.model.AuthenticatedUser;

import java.util.Objects;
import java.util.Optional;

/** Password-free login result suitable for a future controller boundary. */
public final class LoginResult {

    private final LoginStatus status;
    private final AuthenticatedUser user;
    private final String message;

    private LoginResult(LoginStatus status, AuthenticatedUser user, String message) {
        this.status = Objects.requireNonNull(status);
        this.user = user;
        this.message = Objects.requireNonNull(message);
    }

    public static LoginResult success(AuthenticatedUser user) {
        return new LoginResult(LoginStatus.SUCCESS, Objects.requireNonNull(user), "Authentication succeeded.");
    }

    public static LoginResult failure(LoginStatus status, String message) {
        if (status == LoginStatus.SUCCESS) {
            throw new IllegalArgumentException("A failure result cannot use SUCCESS status.");
        }
        return new LoginResult(status, null, message);
    }

    public LoginStatus status() {
        return status;
    }

    public Optional<AuthenticatedUser> user() {
        return Optional.ofNullable(user);
    }

    public String message() {
        return message;
    }

    public boolean successful() {
        return status == LoginStatus.SUCCESS;
    }
}
