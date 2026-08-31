package com.coffeeshop.service;

import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.security.PasswordVerifier;
import com.coffeeshop.security.UserCredentials;

import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Validates credentials and produces a safe authenticated identity. */
public final class AuthenticationService {

    private static final String INVALID_CREDENTIALS = "The username or password is invalid.";

    private final UserRepository userRepository;
    private final PasswordVerifier passwordVerifier;
    private final Clock clock;

    public AuthenticationService(UserRepository userRepository, PasswordVerifier passwordVerifier) {
        this(userRepository, passwordVerifier, Clock.systemUTC());
    }

    AuthenticationService(UserRepository userRepository, PasswordVerifier passwordVerifier, Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordVerifier = Objects.requireNonNull(passwordVerifier);
        this.clock = Objects.requireNonNull(clock);
    }

    public LoginResult login(String username, char[] password) {
        try {
            String normalizedUsername = normalizeUsername(username);
            if (normalizedUsername == null || normalizedUsername.length() > 100
                    || password == null || password.length == 0) {
                return LoginResult.failure(LoginStatus.INVALID_INPUT,
                        "Username and password are required.");
            }

            Optional<UserCredentials> candidate = userRepository.findActiveByUsername(normalizedUsername);
            if (candidate.isEmpty() || !candidate.get().passwordMatches(password, passwordVerifier)) {
                return LoginResult.failure(LoginStatus.INVALID_CREDENTIALS, INVALID_CREDENTIALS);
            }

            UserCredentials credentials = candidate.get();
            if (credentials.isLockedAt(clock.instant())) {
                return LoginResult.failure(LoginStatus.ACCOUNT_LOCKED,
                        "This account is temporarily unavailable. Please contact a manager.");
            }

            Set<RoleInfo> roles = userRepository.findActiveRoles(credentials.userId());
            return LoginResult.success(credentials.toAuthenticatedUser(roles));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
