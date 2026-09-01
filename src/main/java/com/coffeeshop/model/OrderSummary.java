package com.coffeeshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummary(long id, String number, String source, OrderType type,
                           OrderStatus status, LocalDateTime openedAt,
                           BigDecimal total, String tableName) {
    @Override public String toString() { return number + " - " + source + " - " + type + " - " + total; }
}
