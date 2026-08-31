package com.coffeeshop.exception;

/** Indicates a database infrastructure failure safe to surface to application code. */
public final class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
