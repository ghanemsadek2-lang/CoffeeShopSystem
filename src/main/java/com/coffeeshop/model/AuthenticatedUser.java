package com.coffeeshop.model;

import java.util.Objects;
import java.util.Set;

/** Password-free identity retained for the current desktop session. */
public record AuthenticatedUser(long userId, Long employeeId, String username,
                                String displayName, boolean mustChangePassword,
                                Set<RoleInfo> roles) {

    public AuthenticatedUser {
        if (userId <= 0) {
            throw new IllegalArgumentException("User id must be positive.");
        }
        username = requireText(username, "Username");
        displayName = requireText(displayName, "Display name");
        roles = Set.copyOf(Objects.requireNonNull(roles, "Roles are required."));
    }

    public boolean hasRole(String roleCode) {
        Objects.requireNonNull(roleCode, "Role code is required.");
        return roles.stream().anyMatch(role -> role.code().equalsIgnoreCase(roleCode));
    }

    private static String requireText(String value, String label) {
        String result = Objects.requireNonNull(value, label + " is required.").trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        return result;
    }
}
