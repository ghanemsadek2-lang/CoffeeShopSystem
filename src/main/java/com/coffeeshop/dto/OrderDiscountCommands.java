package com.coffeeshop.dto;

public final class OrderDiscountCommands {
    private OrderDiscountCommands() {}

    public record Apply(long orderId, long discountId, byte[] orderRowVersion) {
        public Apply { orderRowVersion = orderRowVersion.clone(); }
        @Override public byte[] orderRowVersion() { return orderRowVersion.clone(); }
    }
}
