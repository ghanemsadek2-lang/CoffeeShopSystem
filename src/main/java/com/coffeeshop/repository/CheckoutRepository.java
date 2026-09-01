package com.coffeeshop.repository;

import com.coffeeshop.dto.CheckoutCommands;
import com.coffeeshop.model.CheckoutModels;

public interface CheckoutRepository {
    CheckoutModels.Context loadContext(long orderId, long userId);
    void complete(CheckoutCommands.Checkout checkout);
}
