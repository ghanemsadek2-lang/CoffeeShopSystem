package com.coffeeshop.dto;

public final class LoyaltyCommands {
    private LoyaltyCommands() {}

    public record Enroll(long customerId, String membershipNumber) {}

    public record ChangePoints(long loyaltyAccountId, String transactionType, long pointsDelta,
                               String reason, String notes, long recordedByUserId,
                               byte[] accountVersion) {
        public ChangePoints { accountVersion = accountVersion.clone(); }
        @Override public byte[] accountVersion() { return accountVersion.clone(); }
    }
}
