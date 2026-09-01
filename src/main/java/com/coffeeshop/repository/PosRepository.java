package com.coffeeshop.repository;
import com.coffeeshop.dto.CreateOrderRequest;
import com.coffeeshop.model.CafeTableInfo;
import com.coffeeshop.model.MenuCategory;
import com.coffeeshop.model.MenuVariant;
import java.util.List;
public interface PosRepository {
    List<MenuCategory> findActiveCategories();
    List<MenuVariant> findActiveMenuVariants();
    List<CafeTableInfo> findAvailableTables();
    String createOpenOrder(CreateOrderRequest request);
}
