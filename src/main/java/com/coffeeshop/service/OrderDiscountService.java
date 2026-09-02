package com.coffeeshop.service;

import com.coffeeshop.dto.OrderDiscountCommands;
import com.coffeeshop.model.OrderDiscountModels;
import com.coffeeshop.repository.OrderDiscountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrderDiscountService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final OrderDiscountRepository repository;

    public OrderDiscountService(OrderDiscountRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public OrderDiscountModels.Context prepare(long orderId) {
        if (orderId <= 0) throw new IllegalArgumentException("Select a valid open order.");
        return repository.prepare(orderId);
    }

    public void apply(OrderDiscountModels.Context context, OrderDiscountModels.DiscountOption discount) {
        Objects.requireNonNull(context, "Discount context is required.");
        Objects.requireNonNull(discount, "Select a discount.");
        if (context.available().stream().noneMatch(value -> value.id() == discount.id()))
            throw new IllegalStateException("The selected discount is not available for this order.");
        if (context.applied().stream().anyMatch(value -> value.code().equals(discount.code())))
            throw new IllegalStateException("This discount is already applied to the order.");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (discount.validFrom() != null && now.isBefore(discount.validFrom())
                || discount.validUntil() != null && !now.isBefore(discount.validUntil()))
            throw new IllegalStateException("The selected discount is outside its validity period.");
        BigDecimal remaining = context.subtotal().subtract(context.discount());
        calculate(discount.type(), discount.value(), remaining);
        repository.apply(new OrderDiscountCommands.Apply(context.orderId(), discount.id(), context.orderRowVersion()));
    }

    public static BigDecimal calculate(String type, BigDecimal value, BigDecimal remainingBase) {
        Objects.requireNonNull(value, "Discount value is required.");
        Objects.requireNonNull(remainingBase, "Remaining order amount is required.");
        if (remainingBase.signum() <= 0) throw new IllegalStateException("The order has no remaining amount to discount.");
        if (value.signum() < 0) throw new IllegalArgumentException("Discount value cannot be negative.");
        BigDecimal amount = switch (type) {
            case "PERCENTAGE" -> {
                if (value.compareTo(ONE_HUNDRED) > 0) throw new IllegalArgumentException("Percentage cannot exceed 100.");
                yield remainingBase.multiply(value).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
            }
            case "FIXED_AMOUNT" -> value.min(remainingBase).setScale(4, RoundingMode.HALF_UP);
            default -> throw new IllegalArgumentException("Unsupported discount type.");
        };
        if (amount.signum() <= 0) throw new IllegalStateException("The selected discount has no value for this order.");
        return amount;
    }

    public static List<BigDecimal> allocate(List<BigDecimal> remainingLines, BigDecimal amount) {
        if (remainingLines == null || remainingLines.isEmpty())
            throw new IllegalStateException("The order has no items to discount.");
        BigDecimal base = remainingLines.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        if (base.signum() <= 0 || amount == null || amount.signum() <= 0 || amount.compareTo(base) > 0)
            throw new IllegalArgumentException("Discount amount is invalid for the order.");
        List<BigDecimal> allocations = new ArrayList<>();
        BigDecimal left = amount.setScale(4, RoundingMode.HALF_UP);
        for (int index = 0; index < remainingLines.size(); index++) {
            BigDecimal capacity = remainingLines.get(index);
            BigDecimal allocation = index == remainingLines.size() - 1 ? left
                    : amount.multiply(capacity).divide(base, 4, RoundingMode.HALF_UP).min(capacity).min(left);
            allocation = allocation.setScale(4, RoundingMode.HALF_UP);
            allocations.add(allocation);
            left = left.subtract(allocation);
        }
        if (left.signum() != 0 || allocations.getLast().compareTo(remainingLines.getLast()) > 0)
            throw new IllegalStateException("Discount allocation could not be reconciled.");
        return List.copyOf(allocations);
    }
}
