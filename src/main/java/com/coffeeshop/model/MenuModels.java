package com.coffeeshop.model;

import java.math.BigDecimal;
import java.util.List;

/** Read models used by menu administration without exposing persistence details to JavaFX. */
public final class MenuModels {
    private MenuModels() { }

    public record Lookup(long id, String code, String name, boolean active) {
        @Override public String toString() { return name + (active ? "" : " (inactive)"); }
    }

    public record Category(long id, String name, String description, int displayOrder,
                           boolean active, byte[] rowVersion) { }

    public record Product(long id, long categoryId, Long stationId, String code, String name,
                          String description, String imagePath, int displayOrder, boolean active,
                          String categoryName, String stationName, byte[] rowVersion) { }

    public record Variant(long id, long productId, Long stationId, String code, String name,
                          String sku, BigDecimal price, int displayOrder, boolean defaultVariant,
                          boolean active, String stationName, byte[] rowVersion) { }

    public record ModifierGroup(long id, String code, String name, String description,
                                int minimumSelections, int maximumSelections, int displayOrder,
                                boolean active, byte[] rowVersion) {
        @Override public String toString() { return name + (active ? "" : " (inactive)"); }
    }

    public record Modifier(long id, long groupId, String code, String name,
                           BigDecimal priceAdjustment, int displayOrder, boolean active,
                           byte[] rowVersion) { }

    public record Assignment(long id, long productId, long groupId, Integer minimumOverride,
                             Integer maximumOverride, int displayOrder, boolean active,
                             String groupName, byte[] rowVersion) { }

    public record Catalog(List<Lookup> categories, List<Lookup> stations, List<Product> products,
                          List<Variant> variants, List<ModifierGroup> groups,
                          List<Modifier> modifiers, List<Assignment> assignments,
                          List<Category> categoryRecords) { }
}
