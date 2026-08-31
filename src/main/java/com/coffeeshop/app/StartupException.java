package com.coffeeshop.app;

import java.util.Objects;

/** Carries a safe user message while retaining the startup failure category. */
public final class StartupException extends RuntimeException {

    private final String userMessage;

    StartupException(String userMessage, Throwable cause) {
        super("Application infrastructure initialization failed.", Objects.requireNonNull(cause));
        this.userMessage = Objects.requireNonNull(userMessage);
    }

    public String userMessage() {
        return userMessage;
    }

    public String failureType() {
        return getCause().getClass().getSimpleName();
    }
}
