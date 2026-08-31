package com.coffeeshop.security;

/** Verifies a supplied password without exposing stored password material. */
@FunctionalInterface
public interface PasswordVerifier {
    boolean matches(char[] password, CharSequence encodedHash);
}
