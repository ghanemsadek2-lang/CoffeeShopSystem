package com.coffeeshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDiscountModels {
    private OrderDiscountModels() {}

    public record DiscountOption(long id, String code, String name, String type, BigDecimal value,
                                 LocalDateTime validFrom, LocalDateTime validUntil) {
        @Override public String toString() {
            return name + " (" + code + ") - " + value.stripTrailingZeros().toPlainString()
                    + ("PERCENTAGE".equals(type) ? "%" : " fixed");
        }
    }

    public record AppliedDiscount(int sequence, String code, String name, String type,
                                  BigDecimal value, BigDecimal appliedAmount) {}

    public record Context(long orderId, String orderNumber, BigDecimal subtotal,
                          BigDecimal discount, BigDecimal tax, BigDecimal total,
                          byte[] orderRowVersion, List<DiscountOption> available,
                          List<AppliedDiscount> applied) {
        public Context {
            orderRowVersion = orderRowVersion.clone();
            available = List.copyOf(available);
            applied = List.copyOf(applied);
        }
        @Override public byte[] orderRowVersion() { return orderRowVersion.clone(); }
    }
}
