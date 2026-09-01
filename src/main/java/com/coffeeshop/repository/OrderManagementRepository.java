package com.coffeeshop.repository;
import com.coffeeshop.model.*;import java.util.List;import java.util.Optional;
public interface OrderManagementRepository{List<OrderSummary> findOrders(OrderStatus status);Optional<OrderDetails> findDetails(long orderId);void cancelOpenOrder(long orderId);List<CafeTableInfo> findTables();}
