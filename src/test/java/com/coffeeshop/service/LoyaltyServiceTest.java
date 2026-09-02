package com.coffeeshop.service;

import com.coffeeshop.dto.LoyaltyCommands;
import com.coffeeshop.model.LoyaltyModels;
import com.coffeeshop.repository.LoyaltyRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoyaltyServiceTest {
    private final StubRepository repository = new StubRepository();
    private final LoyaltyService service = new LoyaltyService(repository);

    @Test void enrollsCustomerWithNormalizedMembershipNumber() {
        service.enroll(new LoyaltyModels.CustomerOption(7, "C7", "Customer"), "  MEMBER-7  ");
        assertEquals(7, repository.enroll.customerId());
        assertEquals("MEMBER-7", repository.enroll.membershipNumber());
    }

    @Test void validatesEnrollment() {
        assertThrows(NullPointerException.class, () -> service.enroll(null, "M1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.enroll(new LoyaltyModels.CustomerOption(1, "C1", "Customer"), " "));
    }

    @Test void earnsPositivePointsWithAuthenticatedAttribution() {
        service.earn(account(20, "ACTIVE"), 15, " Purchase ", " Note ", 9);
        assertEquals("EARN", repository.change.transactionType());
        assertEquals(15, repository.change.pointsDelta());
        assertEquals("Purchase", repository.change.reason());
        assertEquals(9, repository.change.recordedByUserId());
    }

    @Test void redeemsAsSignedNegativeLedgerEntry() {
        service.redeem(account(20, "ACTIVE"), 12, "Reward redemption", null, 9);
        assertEquals("REDEEM", repository.change.transactionType());
        assertEquals(-12, repository.change.pointsDelta());
    }

    @Test void rejectsInvalidPointsAndInsufficientBalance() {
        assertThrows(IllegalArgumentException.class, () -> service.earn(account(20, "ACTIVE"), 0, null, null, 9));
        assertThrows(IllegalArgumentException.class, () -> service.redeem(account(20, "ACTIVE"), 21, null, null, 9));
        assertThrows(IllegalArgumentException.class, () -> service.redeem(account(20, "ACTIVE"), -1, null, null, 9));
        assertThrows(IllegalArgumentException.class, () -> service.earn(account(20, "ACTIVE"), 1, " ", null, 9));
    }

    @Test void rejectsInactiveAccountAndMissingUser() {
        assertThrows(IllegalStateException.class, () -> service.earn(account(20, "SUSPENDED"), 1, "Reason", null, 9));
        assertThrows(IllegalArgumentException.class, () -> service.earn(account(20, "ACTIVE"), 1, "Reason", null, 0));
    }

    private static LoyaltyModels.Account account(long balance, String status) {
        return new LoyaltyModels.Account(1, 2, "M1", "C1", "Customer", balance, 100,
                status, LocalDateTime.now(), new byte[]{1});
    }

    private static final class StubRepository implements LoyaltyRepository {
        LoyaltyCommands.Enroll enroll;
        LoyaltyCommands.ChangePoints change;
        @Override public LoyaltyModels.Catalog load() { return new LoyaltyModels.Catalog(List.of(), List.of(), List.of()); }
        @Override public long enroll(LoyaltyCommands.Enroll command) { enroll = command; return 1; }
        @Override public long changePoints(LoyaltyCommands.ChangePoints command) { change = command; return 1; }
    }
}
