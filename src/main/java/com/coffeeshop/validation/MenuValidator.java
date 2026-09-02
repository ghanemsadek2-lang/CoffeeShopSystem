package com.coffeeshop.validation;

import com.coffeeshop.dto.MenuCommands;

/** Central schema-aligned validation and normalization for menu administration. */
public final class MenuValidator {
    private MenuValidator() { }

    public static MenuCommands.Category category(MenuCommands.Category value) {
        require(value != null, "Category details are required.");
        return new MenuCommands.Category(value.id(), text(value.name(), 100, "Category name"),
                optional(value.description(), 500, "Description"), order(value.displayOrder()),
                value.active(), version(value.id(), value.rowVersion()));
    }

    public static MenuCommands.Product product(MenuCommands.Product value) {
        require(value != null, "Product details are required.");
        require(value.categoryId() > 0, "Select a category.");
        return new MenuCommands.Product(value.id(), value.categoryId(), positiveOrNull(value.stationId(), "Select a valid station."),
                code(value.code(), 50, "Product code"), text(value.name(), 150, "Product name"),
                optional(value.description(), 1000, "Description"), optional(value.imagePath(), 500, "Image path"),
                order(value.displayOrder()), value.active(), version(value.id(), value.rowVersion()));
    }

    public static MenuCommands.Variant variant(MenuCommands.Variant value) {
        require(value != null, "Variant details are required."); require(value.productId() > 0, "Select a product.");
        require(value.price() != null && value.price().signum() >= 0 && value.price().scale() <= 4,
                "Price must be nonnegative with no more than four decimal places.");
        return new MenuCommands.Variant(value.id(), value.productId(), positiveOrNull(value.stationId(), "Select a valid station."),
                code(value.code(), 50, "Variant code"), text(value.name(), 100, "Variant name"),
                optionalCode(value.sku(), 80, "SKU"), value.price(), order(value.displayOrder()),
                value.defaultVariant(), value.active(), version(value.id(), value.rowVersion()));
    }

    public static MenuCommands.Group group(MenuCommands.Group value) {
        require(value != null, "Modifier group details are required.");
        require(value.minimumSelections() >= 0 && value.maximumSelections() >= value.minimumSelections(),
                "Maximum selections must be at least the minimum selections.");
        require(value.maximumSelections() <= Short.MAX_VALUE, "Selection limit is too large.");
        return new MenuCommands.Group(value.id(), code(value.code(), 50, "Group code"),
                text(value.name(), 150, "Group name"), optional(value.description(), 500, "Description"),
                value.minimumSelections(), value.maximumSelections(), order(value.displayOrder()), value.active(),
                version(value.id(), value.rowVersion()));
    }

    public static MenuCommands.Modifier modifier(MenuCommands.Modifier value) {
        require(value != null, "Modifier details are required."); require(value.groupId() > 0, "Select a modifier group.");
        require(value.priceAdjustment() != null && value.priceAdjustment().signum() >= 0 && value.priceAdjustment().scale() <= 4,
                "Price adjustment must be nonnegative with no more than four decimal places.");
        return new MenuCommands.Modifier(value.id(), value.groupId(), code(value.code(), 50, "Modifier code"),
                text(value.name(), 150, "Modifier name"), value.priceAdjustment(), order(value.displayOrder()),
                value.active(), version(value.id(), value.rowVersion()));
    }

    public static MenuCommands.Assignment assignment(MenuCommands.Assignment value) {
        require(value != null, "Assignment details are required.");
        require(value.productId() > 0 && value.groupId() > 0, "Select a product and modifier group.");
        boolean neither=value.minimumOverride()==null&&value.maximumOverride()==null;
        boolean both=value.minimumOverride()!=null&&value.maximumOverride()!=null;
        require(neither||both, "Provide both selection overrides or leave both empty.");
        if(both) require(value.minimumOverride()>=0&&value.maximumOverride()>=value.minimumOverride()
                &&value.maximumOverride()<=Short.MAX_VALUE, "Selection overrides are invalid.");
        return new MenuCommands.Assignment(value.id(), value.productId(), value.groupId(), value.minimumOverride(),
                value.maximumOverride(), order(value.displayOrder()), value.active(), version(value.id(), value.rowVersion()));
    }

    private static String code(String value,int max,String label){String normalized=text(value,max,label);require(!normalized.contains(" "),label+" must not contain spaces.");return normalized;}
    private static String optionalCode(String value,int max,String label){String normalized=optional(value,max,label);if(normalized!=null)require(!normalized.contains(" "),label+" must not contain spaces.");return normalized;}
    private static String text(String value,int max,String label){require(value!=null&&!value.isBlank(),label+" is required.");String normalized=value.trim();require(normalized.length()<=max,label+" is too long.");return normalized;}
    private static String optional(String value,int max,String label){if(value==null||value.isBlank())return null;String normalized=value.trim();require(normalized.length()<=max,label+" is too long.");return normalized;}
    private static int order(int value){require(value>=0,"Display order cannot be negative.");return value;}
    private static Long positiveOrNull(Long value,String message){require(value==null||value>0,message);return value;}
    private static byte[] version(Long id,byte[] value){require(id==null||(value!=null&&value.length>0),"This record must be refreshed before editing.");return value;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
