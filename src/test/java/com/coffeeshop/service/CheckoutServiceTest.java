package com.coffeeshop.service;

import com.coffeeshop.dto.CheckoutCommands;
import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.CheckoutModels;
import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.repository.CheckoutRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private final Stub repository = new Stub();
    private final CheckoutService service = new CheckoutService(repository);
    private final CheckoutModels.PaymentMethod cash = new CheckoutModels.PaymentMethod(1, "CASH", "Cash");
    private final CheckoutModels.PaymentMethod card = new CheckoutModels.PaymentMethod(2, "CARD", "Card");

    @Test void preparesCheckoutForAuthenticatedUser() {
        CheckoutModels.Context expected = context(9L);
        repository.context = expected;
        assertSame(expected, service.prepare(4, user()));
        assertEquals(4, repository.preparedOrderId);
        assertEquals(7, repository.preparedUserId);
    }

    @Test void acceptsExactSinglePayment() {
        CheckoutCommands.Checkout value = service.validate(command(payment(2, "12.50")), context(null));
        assertEquals(money("12.5000"), value.payments().getFirst().amount());
    }

    @Test void acceptsExactSplitPayment() {
        CheckoutCommands.Checkout value = service.validate(
                command(payment(1, "5.00"), payment(2, "7.50")), context(9L));
        assertEquals(2, value.payments().size());
    }

    @Test void rejectsUnderpaymentAndOverpayment() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(command(payment(2, "12.49")), context(null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(command(payment(2, "12.51")), context(null)));
    }

    @Test void rejectsZeroNegativeAndExcessScale() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(command(payment(2, "0")), context(null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(command(payment(2, "-1")), context(null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(command(payment(2, "12.50001")), context(null)));
    }

    @Test void cashRequiresOpenAuthenticatedUserShift() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.validate(command(payment(1, "12.50")), context(null)));
        assertTrue(error.getMessage().contains("register shift"));
    }

    @Test void cardDoesNotRequireOpenShift() {
        assertDoesNotThrow(() -> service.validate(command(payment(2, "12.50")), context(null)));
    }

    @Test void rejectsInactiveOrUnknownPaymentMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validate(command(payment(99, "12.50")), context(9L)));
    }

    @Test void rejectsStaleCheckoutContext() {
        CheckoutCommands.Checkout stale = new CheckoutCommands.Checkout(
                4, 7, money("12.50"), new byte[]{9}, List.of(payment(2, "12.50")));
        assertThrows(IllegalStateException.class, () -> service.validate(stale, context(null)));
    }

    @Test void completeValidatesAndDelegatesAtomicallyToRepositoryBoundary() {
        service.complete(command(payment(2, "12.50")));
        assertNotNull(repository.completed);
        assertEquals(money("12.5000"), repository.completed.expectedTotal());
    }

    @Test void repositoryConcurrencyAndStockFailuresArePreserved() {
        repository.failure = new IllegalStateException("Inventory changed during checkout.");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.complete(command(payment(2, "12.50"))));
        assertEquals("Inventory changed during checkout.", error.getMessage());
    }

    private CheckoutModels.Context context(Long shiftId) {
        return new CheckoutModels.Context(4, "ORD00000004", money("12.50"), new byte[]{1, 2},
                List.of(cash, card), shiftId, shiftId == null ? null : "Front Register");
    }

    private CheckoutCommands.Checkout command(CheckoutCommands.Payment... payments) {
        return new CheckoutCommands.Checkout(4, 7, money("12.50"), new byte[]{1, 2}, List.of(payments));
    }

    private static CheckoutCommands.Payment payment(long methodId, String amount) {
        return new CheckoutCommands.Payment(methodId, new BigDecimal(amount), null);
    }

    private static BigDecimal money(String value) { return new BigDecimal(value).setScale(4); }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(7, 3L, "cashier", "Cashier", false,
                Set.of(new RoleInfo(1, "CASHIER", "Cashier")));
    }

    private static final class Stub implements CheckoutRepository {
        private CheckoutModels.Context context;
        private CheckoutCommands.Checkout completed;
        private RuntimeException failure;
        private long preparedOrderId;
        private long preparedUserId;

        @Override public CheckoutModels.Context loadContext(long orderId, long userId) {
            preparedOrderId = orderId;
            preparedUserId = userId;
            return context;
        }

        @Override public void complete(CheckoutCommands.Checkout checkout) {
            if (failure != null) throw failure;
            completed = checkout;
        }
    }
}
