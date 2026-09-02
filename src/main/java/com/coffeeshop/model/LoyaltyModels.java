package com.coffeeshop.model;

import java.time.LocalDateTime;
import java.util.List;

public final class LoyaltyModels {
    private LoyaltyModels() {}

    public record Account(long id, long customerId, String membershipNumber, String customerNumber,
                          String customerName, long pointsBalance, long lifetimePointsEarned,
                          String status, LocalDateTime enrolledAt, byte[] rowVersion) {
        public Account { rowVersion = rowVersion.clone(); }
        @Override public byte[] rowVersion() { return rowVersion.clone(); }
        @Override public String toString() { return customerName + " (" + membershipNumber + ")"; }
    }

    public record CustomerOption(long id, String number, String name) {
        @Override public String toString() { return name + " (" + number + ")"; }
    }

    public record Transaction(long id, long accountId, String membershipNumber, String customerName,
                              String type, long pointsDelta, String recordedBy, String reason,
                              String notes, LocalDateTime occurredAt) {}

    public record Catalog(List<Account> accounts, List<CustomerOption> availableCustomers,
                          List<Transaction> transactions) {
        public Catalog {
            accounts = List.copyOf(accounts);
            availableCustomers = List.copyOf(availableCustomers);
            transactions = List.copyOf(transactions);
        }
    }
}
