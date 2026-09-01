package com.coffeeshop.dto;
import com.coffeeshop.model.CartLine;
import com.coffeeshop.model.OrderType;
import java.util.List;
public record CreateOrderRequest(long userId, OrderType orderType, Long cafeTableId,
                                 DeliveryInfo deliveryInfo, List<CartLine> lines) {
    public CreateOrderRequest { lines=List.copyOf(lines); }
}
