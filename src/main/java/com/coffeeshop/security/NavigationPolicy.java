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
            NavigationItem.TABLES, NavigationItem.REGISTERS, NavigationItem.CUSTOMERS,
            NavigationItem.LOYALTY, NavigationItem.DOCUMENTS, NavigationItem.NOTIFICATIONS
    ));

    private static final Set<NavigationItem> MANAGER_RESTRICTED_ITEMS = Set.copyOf(EnumSet.of(
            NavigationItem.EMPLOYEES, NavigationItem.AUDIT_LOG, NavigationItem.SETTINGS
    ));

    public Set<NavigationItem> allowedItems(AuthenticatedUser user) {
        Objects.requireNonNull(user);
        if (user.hasRole("ADMIN")) {
            return Set.copyOf(EnumSet.allOf(NavigationItem.class));
        }
        if (user.hasRole("MANAGER")) {
            EnumSet<NavigationItem> items = EnumSet.allOf(NavigationItem.class);
            items.removeAll(MANAGER_RESTRICTED_ITEMS);
            return Set.copyOf(items);
        }
        if (user.hasRole("CASHIER")) {
            return CASHIER_ITEMS;
        }
        return Set.of();
    }

    public NavigationItem defaultItem(AuthenticatedUser user) {
        Objects.requireNonNull(user);
        if (user.hasRole("ADMIN") || user.hasRole("MANAGER")) {
            return NavigationItem.DASHBOARD;
        }
        if (user.hasRole("CASHIER")) {
            return NavigationItem.POS;
        }
        throw new SecurityException("The current user has no recognized application role.");
    }

    public boolean isCashierWorkspace(AuthenticatedUser user) {
        Objects.requireNonNull(user);
        return !user.hasRole("ADMIN") && !user.hasRole("MANAGER") && user.hasRole("CASHIER");
    }

    public boolean isManagerWorkspace(AuthenticatedUser user) {
        Objects.requireNonNull(user);
        return !user.hasRole("ADMIN") && user.hasRole("MANAGER");
    }

    public boolean canAccess(AuthenticatedUser user, NavigationItem item) {
        return allowedItems(user).contains(Objects.requireNonNull(item));
    }

    /** Enforces the same access policy at the view-loading boundary as in the sidebar. */
    public void requireAccess(AuthenticatedUser user, NavigationItem item) {
        if (!canAccess(user, item)) {
            throw new SecurityException("The current user is not authorized to open this module.");
        }
    }
}
