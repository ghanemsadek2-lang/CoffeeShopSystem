package com.coffeeshop.repository;

import com.coffeeshop.dto.OrderDiscountCommands;
import com.coffeeshop.model.OrderDiscountModels;

public interface OrderDiscountRepository {
    OrderDiscountModels.Context prepare(long orderId);
    void apply(OrderDiscountCommands.Apply command);
}
