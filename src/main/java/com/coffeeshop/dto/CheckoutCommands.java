package com.coffeeshop.dto;

import java.math.BigDecimal;
import java.util.List;

/** Validated checkout input passed from the UI to the transaction boundary. */
public final class CheckoutCommands {
    private CheckoutCommands() { }

    public record Payment(long paymentMethodId, BigDecimal amount, String externalReference) { }

    public record Checkout(long orderId, long userId, BigDecimal expectedTotal,
                           byte[] orderRowVersion, List<Payment> payments) {
        public Checkout {
            orderRowVersion = orderRowVersion == null ? null : orderRowVersion.clone();
            payments = payments == null ? null : List.copyOf(payments);
        }

        @Override public byte[] orderRowVersion() {
            return orderRowVersion == null ? null : orderRowVersion.clone();
        }
    }
}
