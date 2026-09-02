package com.coffeeshop.service;

import com.coffeeshop.dto.LoyaltyCommands;
import com.coffeeshop.model.LoyaltyModels;
import com.coffeeshop.repository.LoyaltyRepository;
import com.coffeeshop.validation.LoyaltyValidator;

import java.util.Objects;

public final class LoyaltyService {
    private final LoyaltyRepository repository;

    public LoyaltyService(LoyaltyRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public LoyaltyModels.Catalog catalog() { return repository.load(); }

    public long enroll(LoyaltyModels.CustomerOption customer, String membershipNumber) {
        Objects.requireNonNull(customer, "Select a customer.");
        return repository.enroll(new LoyaltyCommands.Enroll(customer.id(),
                LoyaltyValidator.membershipNumber(membershipNumber)));
    }

    public long earn(LoyaltyModels.Account account, long points, String reason, String notes, long userId) {
        return change(account, "EARN", LoyaltyValidator.positivePoints(points), reason, notes, userId);
    }

    public long redeem(LoyaltyModels.Account account, long points, String reason, String notes, long userId) {
        long amount = LoyaltyValidator.positivePoints(points);
        if (account != null && amount > account.pointsBalance()) {
            throw new IllegalArgumentException("Redeemed points exceed the available balance.");
        }
        return change(account, "REDEEM", -amount, reason, notes, userId);
    }

    private long change(LoyaltyModels.Account account, String type, long delta,
                        String reason, String notes, long userId) {
        Objects.requireNonNull(account, "Select a loyalty account.");
        if (!"ACTIVE".equals(account.status())) throw new IllegalStateException("The loyalty account is not active.");
        if (userId <= 0) throw new IllegalArgumentException("An authenticated user is required.");
        return repository.changePoints(new LoyaltyCommands.ChangePoints(account.id(), type, delta,
                LoyaltyValidator.reason(reason),
                LoyaltyValidator.optional(notes, 1000, "Notes"), userId, account.rowVersion()));
    }
}
