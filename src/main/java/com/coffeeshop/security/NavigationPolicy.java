package com.coffeeshop.security;

import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.NavigationItem;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Centralizes role-aware shell navigation independently of JavaFX controls. */
public final class NavigationPolicy {

    private static final Set<NavigationItem> CASHIER_ITEMS = Set.copyOf(EnumSet.of(
            NavigationItem.DASHBOARD, NavigationItem.POS, NavigationItem.ORDERS,
            NavigationItem.TABLES, NavigationItem.CUSTOMERS
    ));

    public Set<NavigationItem> allowedItems(AuthenticatedUser user) {
        Objects.requireNonNull(user);
        if (user.hasRole("ADMIN")) {
            return Set.copyOf(EnumSet.allOf(NavigationItem.class));
        }
        if (user.hasRole("MANAGER")) {
            EnumSet<NavigationItem> items = EnumSet.allOf(NavigationItem.class);
            items.remove(NavigationItem.SETTINGS);
            return Set.copyOf(items);
        }
        return CASHIER_ITEMS;
    }

    public boolean canAccess(AuthenticatedUser user, NavigationItem item) {
        return allowedItems(user).contains(Objects.requireNonNull(item));
    }
}
