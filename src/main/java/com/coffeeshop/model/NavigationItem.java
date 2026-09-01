package com.coffeeshop.model;

/** Stable destinations in the authenticated application shell. */
public enum NavigationItem {
    DASHBOARD("Dashboard"),
    POS("POS / New Order"),
    ORDERS("Orders"),
    TABLES("Tables"),
    PRODUCTS("Products / Menu"),
    INVENTORY("Inventory"),
    CUSTOMERS("Customers"),
    EMPLOYEES("Employees / Users"),
    PURCHASING("Suppliers / Purchases"),
    EXPENSES("Expenses"),
    REPORTS("Reports"),
    SETTINGS("Settings");

    private final String label;

    NavigationItem(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
