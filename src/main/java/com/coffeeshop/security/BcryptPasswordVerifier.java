package com.coffeeshop.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.Objects;

/** Password verifier backed by bcrypt. */
public final class BcryptPasswordVerifier implements PasswordVerifier {

    @Override
    public boolean matches(char[] password, CharSequence encodedHash) {
        Objects.requireNonNull(password, "Password is required.");
        Objects.requireNonNull(encodedHash, "Encoded password hash is required.");
        return BCrypt.verifyer().verify(password, encodedHash).verified;
    }
}
