package com.coffeeshop.service;
import com.coffeeshop.model.*;import com.coffeeshop.repository.OrderManagementRepository;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class OrderManagementServiceTest{
 private final Stub repo=new Stub();private final OrderManagementService service=new OrderManagementService(repo);private OrderSummary order(OrderStatus s){return new OrderSummary(1,"ORD1","POS",OrderType.DINE_IN,s,LocalDateTime.now(),BigDecimal.TEN,"Table 1");}
 @Test void onlyOpenToCancelledIsAllowed(){assertTrue(service.canTransition(OrderStatus.OPEN,OrderStatus.CANCELLED));assertFalse(service.canTransition(OrderStatus.OPEN,OrderStatus.COMPLETED));assertFalse(service.canTransition(OrderStatus.COMPLETED,OrderStatus.CANCELLED));}
 @Test void cancelDelegatesForOpenOrder(){service.cancel(order(OrderStatus.OPEN));assertEquals(1,repo.cancelled);}
 @Test void completedOrderCannotBeCancelled(){assertThrows(IllegalStateException.class,()->service.cancel(order(OrderStatus.COMPLETED)));}
 private static final class Stub implements OrderManagementRepository{long cancelled;public List<OrderSummary>findOrders(OrderStatus s){return List.of();}public Optional<OrderDetails>findDetails(long id){return Optional.empty();}public void cancelOpenOrder(long id){cancelled=id;}public List<CafeTableInfo>findTables(){return List.of();}}
}
