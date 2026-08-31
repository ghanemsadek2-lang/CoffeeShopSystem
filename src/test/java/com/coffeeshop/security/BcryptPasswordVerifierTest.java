package com.coffeeshop.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordVerifierTest {

    private final BcryptPasswordVerifier verifier = new BcryptPasswordVerifier();

    @Test
    void verifiesBcryptWithoutPlaintextComparison() {
        char[] expected = "correct horse battery staple".toCharArray();
        String hash = BCrypt.withDefaults().hashToString(4, expected);

        assertTrue(verifier.matches(expected, hash));
        assertFalse(verifier.matches("incorrect".toCharArray(), hash));
    }
}
