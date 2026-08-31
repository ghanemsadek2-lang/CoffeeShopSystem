package com.coffeeshop.model;

import java.util.Objects;

/** Safe role identity exposed to authenticated application code. */
public record RoleInfo(long id, String code, String name) {

    public RoleInfo {
        if (id <= 0) {
            throw new IllegalArgumentException("Role id must be positive.");
        }
        code = requireText(code, "Role code");
        name = requireText(name, "Role name");
    }

    private static String requireText(String value, String label) {
        String result = Objects.requireNonNull(value, label + " is required.").trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        return result;
    }
}
