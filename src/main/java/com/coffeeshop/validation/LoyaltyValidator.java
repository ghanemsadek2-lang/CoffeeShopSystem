package com.coffeeshop.validation;

public final class LoyaltyValidator {
    private LoyaltyValidator() {}

    public static String membershipNumber(String value) {
        return required(value, 50, "Membership number");
    }

    public static long positivePoints(long points) {
        if (points <= 0) throw new IllegalArgumentException("Points must be greater than zero.");
        return points;
    }

    public static String reason(String value) { return required(value, 500, "Reason"); }

    public static String optional(String value, int max, String label) {
        return value == null || value.isBlank() ? null : required(value, max, label);
    }

    private static String required(String value, int max, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        String normalized = value.trim();
        if (normalized.length() > max) throw new IllegalArgumentException(label + " is too long.");
        return normalized;
    }
}
