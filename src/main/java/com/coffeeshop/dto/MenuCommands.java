package com.coffeeshop.dto;

import java.math.BigDecimal;

/** Validated menu write requests. IDs are null for new records. */
public final class MenuCommands {
    private MenuCommands() { }

    public record Category(Long id, String name, String description, int displayOrder,
                           boolean active, byte[] rowVersion) { }

    public record Product(Long id, long categoryId, Long stationId, String code, String name,
                          String description, String imagePath, int displayOrder, boolean active,
                          byte[] rowVersion) { }
    public record Variant(Long id, long productId, Long stationId, String code, String name,
                          String sku, BigDecimal price, int displayOrder, boolean defaultVariant,
                          boolean active, byte[] rowVersion) { }
    public record Group(Long id, String code, String name, String description, int minimumSelections,
                        int maximumSelections, int displayOrder, boolean active, byte[] rowVersion) { }
    public record Modifier(Long id, long groupId, String code, String name, BigDecimal priceAdjustment,
                           int displayOrder, boolean active, byte[] rowVersion) { }
    public record Assignment(Long id, long productId, long groupId, Integer minimumOverride,
                             Integer maximumOverride, int displayOrder, boolean active,
                             byte[] rowVersion) { }
}
