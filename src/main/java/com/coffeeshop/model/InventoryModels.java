package com.coffeeshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read models for inventory administration and recipe configuration. */
public final class InventoryModels {
    private InventoryModels() { }

    public record Lookup(long id,String code,String name,boolean active) {
        @Override public String toString(){return name+(active?"":" (inactive)");}
    }
    public record Item(long id,long unitId,String code,String name,BigDecimal stock,
                       BigDecimal reorderLevel,boolean allowNegative,boolean active,
                       String unitName,byte[] rowVersion) {
        @Override public String toString(){return name+" · "+unitName+(active?"":" (inactive)");}
    }
    public record Movement(long id,long itemId,String type,BigDecimal quantityDelta,
                           String reason,String notes,LocalDateTime occurredAt,String recordedBy) { }
    public record Variant(long id,long productId,String productName,String variantName,boolean active) {
        @Override public String toString(){return productName+" · "+variantName+(active?"":" (inactive)");}
    }
    public record Modifier(long id,String groupName,String name,boolean active) {
        @Override public String toString(){return groupName+" · "+name+(active?"":" (inactive)");}
    }
    public record RecipeItem(long id,long variantId,long inventoryItemId,String inventoryItemName,
                             String unitName,BigDecimal quantityRequired,byte[] rowVersion) { }
    public record ModifierRecipeItem(long id,long modifierId,long inventoryItemId,String inventoryItemName,
                                     String unitName,BigDecimal quantityRequired,byte[] rowVersion) { }
    public record Catalog(List<Lookup> units,List<Item> items,List<Movement> movements,
                          List<Variant> variants,List<Modifier> modifiers,List<RecipeItem> recipes,
                          List<ModifierRecipeItem> modifierRecipes) {
        public Catalog { units=List.copyOf(units);items=List.copyOf(items);movements=List.copyOf(movements);variants=List.copyOf(variants);modifiers=List.copyOf(modifiers);recipes=List.copyOf(recipes);modifierRecipes=List.copyOf(modifierRecipes); }
    }
}
