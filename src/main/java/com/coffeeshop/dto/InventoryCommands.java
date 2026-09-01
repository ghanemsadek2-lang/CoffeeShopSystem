package com.coffeeshop.dto;

import java.math.BigDecimal;

/** Inventory writes after UI capture and before centralized validation. */
public final class InventoryCommands {
    private InventoryCommands() { }
    public record Item(Long id,long unitId,String code,String name,BigDecimal reorderLevel,
                       boolean allowNegative,boolean active,byte[] rowVersion) { }
    public record Adjustment(long itemId,long userId,BigDecimal quantityDelta,String reason,String notes) { }
    public record Recipe(Long id,long variantId,long inventoryItemId,BigDecimal quantityRequired,byte[] rowVersion) { }
    public record ModifierRecipe(Long id,long modifierId,long inventoryItemId,BigDecimal quantityRequired,byte[] rowVersion) { }
}
