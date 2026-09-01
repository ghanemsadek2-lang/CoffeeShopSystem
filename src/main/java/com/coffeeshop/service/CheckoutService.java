package com.coffeeshop.service;

import com.coffeeshop.dto.CheckoutCommands;
import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.CheckoutModels;
import com.coffeeshop.repository.CheckoutRepository;
import com.coffeeshop.validation.CheckoutValidator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Checkout orchestration; the repository owns the single sale transaction. */
public final class CheckoutService {
    private final CheckoutRepository repository;

    public CheckoutService(CheckoutRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public CheckoutModels.Context prepare(long orderId, AuthenticatedUser user) {
        Objects.requireNonNull(user, "Authenticated user is required.");
        if (orderId <= 0) throw new IllegalArgumentException("Select a valid open order.");
        return repository.loadContext(orderId, user.userId());
    }

    public CheckoutCommands.Checkout validate(CheckoutCommands.Checkout checkout) {
        return CheckoutValidator.checkout(checkout);
    }

    public CheckoutCommands.Checkout validate(CheckoutCommands.Checkout checkout,
                                               CheckoutModels.Context context) {
        Objects.requireNonNull(context, "Checkout context is required.");
        CheckoutCommands.Checkout normalized = validate(checkout);
        if (normalized.orderId() != context.orderId()
                || normalized.expectedTotal().compareTo(context.total()) != 0
                || !java.util.Arrays.equals(normalized.orderRowVersion(), context.orderRowVersion()))
            throw new IllegalStateException("The order changed. Refresh and try again.");
        Map<Long, CheckoutModels.PaymentMethod> methods = context.paymentMethods().stream()
                .collect(Collectors.toMap(CheckoutModels.PaymentMethod::id, Function.identity()));
        for (CheckoutCommands.Payment payment : normalized.payments()) {
            CheckoutModels.PaymentMethod method = methods.get(payment.paymentMethodId());
            if (method == null) throw new IllegalArgumentException("Select an active payment method.");
            if ("CASH".equals(method.code()) && !context.hasOpenShift())
                throw new IllegalStateException("Open a register shift before accepting cash.");
        }
        return normalized;
    }

    public void complete(CheckoutCommands.Checkout checkout) {
        repository.complete(validate(checkout));
    }
}
