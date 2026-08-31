package com.coffeeshop.security;

import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.RoleInfo;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** Authentication-only user data; the stored hash has no accessor. */
public final class UserCredentials {

    private final long userId;
    private final Long employeeId;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final boolean mustChangePassword;
    private final Instant lockedUntil;

    public UserCredentials(long userId, Long employeeId, String username, String displayName,
                           String passwordHash, boolean mustChangePassword, Instant lockedUntil) {
        this.userId = userId;
        this.employeeId = employeeId;
        this.username = Objects.requireNonNull(username);
        this.displayName = Objects.requireNonNull(displayName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.mustChangePassword = mustChangePassword;
        this.lockedUntil = lockedUntil;
    }

    public boolean passwordMatches(char[] password, PasswordVerifier verifier) {
        return Objects.requireNonNull(verifier).matches(password, passwordHash);
    }

    public boolean isLockedAt(Instant instant) {
        return lockedUntil != null && lockedUntil.isAfter(Objects.requireNonNull(instant));
    }

    public long userId() {
        return userId;
    }

    public AuthenticatedUser toAuthenticatedUser(Set<RoleInfo> roles) {
        return new AuthenticatedUser(userId, employeeId, username, displayName,
                mustChangePassword, roles);
    }
}
