package com.coffeeshop.service;
import com.coffeeshop.model.*;import com.coffeeshop.repository.OrderManagementRepository;import java.util.*;
public final class OrderManagementService{
 private final OrderManagementRepository repository;public OrderManagementService(OrderManagementRepository r){repository=Objects.requireNonNull(r);}public List<OrderSummary>orders(OrderStatus s){return repository.findOrders(s);}public Optional<OrderDetails>details(long id){return repository.findDetails(id);}public List<CafeTableInfo>tables(){return repository.findTables();}
 public boolean canTransition(OrderStatus from,OrderStatus to){return from==OrderStatus.OPEN&&to==OrderStatus.CANCELLED;}public void cancel(OrderSummary o){if(!canTransition(o.status(),OrderStatus.CANCELLED))throw new IllegalStateException("Only open orders can be cancelled.");repository.cancelOpenOrder(o.id());}
}
