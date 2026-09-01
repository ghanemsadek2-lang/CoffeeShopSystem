package com.coffeeshop.model;
import java.math.BigDecimal;
public record MenuVariant(long variantId, long productId, long categoryId, String productName,
                          String variantName, BigDecimal price, Long stationId,
                          String stationCode, String stationName) {
    @Override public String toString() { return productName + " · " + variantName; }
}
