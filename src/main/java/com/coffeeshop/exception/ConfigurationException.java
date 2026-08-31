package com.coffeeshop.exception;

/** Indicates that external application configuration is missing or invalid. */
public final class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
