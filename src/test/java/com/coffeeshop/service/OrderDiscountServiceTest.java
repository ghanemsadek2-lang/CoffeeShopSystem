package com.coffeeshop.service;

import com.coffeeshop.dto.OrderDiscountCommands;
import com.coffeeshop.model.OrderDiscountModels;
import com.coffeeshop.repository.OrderDiscountRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderDiscountServiceTest {
    private final Stub repository = new Stub();
    private final OrderDiscountService service = new OrderDiscountService(repository);

    @Test void calculatesPercentageAgainstRemainingBase() {
        assertEquals(money("8.0000"), OrderDiscountService.calculate("PERCENTAGE", money("10"), money("80")));
    }

    @Test void capsFixedDiscountAtRemainingBase() {
        assertEquals(money("20.0000"), OrderDiscountService.calculate("FIXED_AMOUNT", money("50"), money("20")));
    }

    @Test void allocatesAndReconcilesAtFourDecimals() {
        List<BigDecimal> values = OrderDiscountService.allocate(
                List.of(money("10"), money("20"), money("30")), money("10"));
        assertEquals(money("10"), values.stream().reduce(money("0"), BigDecimal::add));
        assertEquals(3, values.size());
    }

    @Test void appliesSelectedDiscountWithOrderVersion() {
        OrderDiscountModels.Context context = context(option("PERCENTAGE", "10", null, null));
        service.apply(context, context.available().getFirst());
        assertEquals(4, repository.command.orderId());
        assertEquals(7, repository.command.discountId());
        assertArrayEquals(new byte[]{1, 2}, repository.command.orderRowVersion());
    }

    @Test void rejectsExpiredAndNotYetValidDiscounts() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        assertThrows(IllegalStateException.class, () -> service.apply(
                context(option("FIXED_AMOUNT", "5", null, now.minusSeconds(1))),
                option("FIXED_AMOUNT", "5", null, now.minusSeconds(1))));
        assertThrows(IllegalStateException.class, () -> service.apply(
                context(option("FIXED_AMOUNT", "5", now.plusDays(1), null)),
                option("FIXED_AMOUNT", "5", now.plusDays(1), null)));
    }

    @Test void rejectsUnavailableAndDuplicateApplication() {
        OrderDiscountModels.DiscountOption discount = option("FIXED_AMOUNT", "5", null, null);
        OrderDiscountModels.Context unavailable = new OrderDiscountModels.Context(4, "ORD4", money("100"),
                money("0"), money("0"), money("100"), new byte[]{1}, List.of(), List.of());
        assertThrows(IllegalStateException.class, () -> service.apply(unavailable, discount));
        OrderDiscountModels.Context duplicate = new OrderDiscountModels.Context(4, "ORD4", money("100"),
                money("5"), money("0"), money("95"), new byte[]{1}, List.of(discount),
                List.of(new OrderDiscountModels.AppliedDiscount(1, "D7", "Discount", "FIXED_AMOUNT", money("5"), money("5"))));
        assertThrows(IllegalStateException.class, () -> service.apply(duplicate, discount));
    }

    @Test void rejectsZeroValueAndFullyDiscountedOrder() {
        assertThrows(IllegalStateException.class,
                () -> OrderDiscountService.calculate("PERCENTAGE", money("0"), money("10")));
        assertThrows(IllegalStateException.class,
                () -> OrderDiscountService.calculate("FIXED_AMOUNT", money("1"), money("0")));
    }

    private static OrderDiscountModels.Context context(OrderDiscountModels.DiscountOption option) {
        return new OrderDiscountModels.Context(4, "ORD4", money("100"), money("20"), money("0"),
                money("80"), new byte[]{1, 2}, List.of(option), List.of());
    }
    private static OrderDiscountModels.DiscountOption option(String type, String value,
                                                              LocalDateTime from, LocalDateTime until) {
        return new OrderDiscountModels.DiscountOption(7, "D7", "Discount", type, money(value), from, until);
    }
    private static BigDecimal money(String value) { return new BigDecimal(value).setScale(4); }
    private static final class Stub implements OrderDiscountRepository {
        OrderDiscountCommands.Apply command;
        @Override public OrderDiscountModels.Context prepare(long orderId) { return null; }
        @Override public void apply(OrderDiscountCommands.Apply value) { command = value; }
    }
}
