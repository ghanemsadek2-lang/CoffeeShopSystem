package com.coffeeshop.service;
import com.coffeeshop.dto.*;import com.coffeeshop.model.*;import com.coffeeshop.repository.PosRepository;
import java.math.BigDecimal;import java.math.RoundingMode;import java.util.*;
/** Validates cashier order drafts before transactional persistence. */
public final class PosService {
 private final PosRepository repository; public PosService(PosRepository r){repository=Objects.requireNonNull(r);}
 public List<MenuCategory> categories(){return repository.findActiveCategories();} public List<MenuVariant> menu(){return repository.findActiveMenuVariants();} public List<CafeTableInfo> availableTables(){return repository.findAvailableTables();}
 public BigDecimal subtotal(Collection<CartLine> lines){return lines.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(4,RoundingMode.HALF_UP);}
 public String saveOpenOrder(CreateOrderRequest q){Objects.requireNonNull(q);if(q.userId()<=0)throw new IllegalArgumentException("An authenticated user is required.");if(q.lines().isEmpty())throw new IllegalArgumentException("Add at least one item.");if(q.orderType()==OrderType.DINE_IN&&q.cafeTableId()==null)throw new IllegalArgumentException("Select an available table.");if(q.orderType()==OrderType.DELIVERY)validateDelivery(q.deliveryInfo());return repository.createOpenOrder(q);}
 private static void validateDelivery(DeliveryInfo d){if(d==null||blank(d.recipientName())||blank(d.phone())||blank(d.addressLine1())||blank(d.city()))throw new IllegalArgumentException("Complete all required delivery details.");}
 private static boolean blank(String s){return s==null||s.isBlank();}
}
