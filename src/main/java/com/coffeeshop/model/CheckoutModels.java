package com.coffeeshop.model;

import java.math.BigDecimal;
import java.util.List;

/** Read models needed to prepare an order checkout. */
public final class CheckoutModels {
    private CheckoutModels() { }

    public record PaymentMethod(long id, String code, String name) {
        @Override public String toString() { return name; }
    }

    public record Context(long orderId, String orderNumber, BigDecimal total,
                          byte[] orderRowVersion, List<PaymentMethod> paymentMethods,
                          Long currentShiftId, String currentRegisterName) {
        public Context {
            orderRowVersion = orderRowVersion == null ? null : orderRowVersion.clone();
            paymentMethods = List.copyOf(paymentMethods);
        }

        @Override public byte[] orderRowVersion() {
            return orderRowVersion == null ? null : orderRowVersion.clone();
        }

        public boolean hasOpenShift() { return currentShiftId != null; }
    }
}
