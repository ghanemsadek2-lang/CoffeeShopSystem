package com.coffeeshop.validation;

import com.coffeeshop.dto.CheckoutCommands;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Schema-aligned validation and normalization for checkout tenders. */
public final class CheckoutValidator {
    private CheckoutValidator() { }

    public static CheckoutCommands.Checkout checkout(CheckoutCommands.Checkout value) {
        require(value != null, "Checkout details are required.");
        require(value.orderId() > 0, "Select a valid open order.");
        require(value.userId() > 0, "An authenticated user is required.");
        BigDecimal expected = money(value.expectedTotal(), false, "Order total");
        require(value.orderRowVersion() != null && value.orderRowVersion().length > 0,
                "Refresh the order before completing it.");
        require(value.payments() != null && !value.payments().isEmpty(),
                "Add at least one payment.");

        List<CheckoutCommands.Payment> payments = new ArrayList<>();
        BigDecimal paid = BigDecimal.ZERO.setScale(4);
        for (CheckoutCommands.Payment payment : value.payments()) {
            require(payment != null, "Payment details are required.");
            require(payment.paymentMethodId() > 0, "Select a payment method.");
            BigDecimal amount = money(payment.amount(), false, "Payment amount");
            String reference = optional(payment.externalReference(), 200, "Payment reference");
            payments.add(new CheckoutCommands.Payment(payment.paymentMethodId(), amount, reference));
            paid = paid.add(amount);
        }
        require(paid.compareTo(expected) == 0,
                "Payments must equal the order total exactly.");
        return new CheckoutCommands.Checkout(value.orderId(), value.userId(), expected,
                value.orderRowVersion(), payments);
    }

    private static BigDecimal money(BigDecimal value, boolean allowZero, String label) {
        require(value != null, label + " is required.");
        require(value.scale() <= 4, label + " must have no more than four decimal places.");
        require(value.precision() - value.scale() <= 15, label + " is too large.");
        require(allowZero ? value.signum() >= 0 : value.signum() > 0,
                label + (allowZero ? " cannot be negative." : " must be greater than zero."));
        return value.setScale(4);
    }

    private static String optional(String value, int max, String label) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        require(normalized.length() <= max, label + " is too long.");
        return normalized;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
