package com.coffeeshop.security;

import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.NavigationItem;
import com.coffeeshop.model.RoleInfo;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class NavigationPolicyTest {
    private final NavigationPolicy policy = new NavigationPolicy();

    @Test void adminCanAccessEveryDestination() {
        assertEquals(Set.of(NavigationItem.values()), policy.allowedItems(user("ADMIN")));
    }

    @Test void cashierReceivesOperationalDestinationsOnly() {
        var allowed = policy.allowedItems(user("CASHIER"));
        assertTrue(allowed.containsAll(Set.of(NavigationItem.POS, NavigationItem.ORDERS,
                NavigationItem.TABLES, NavigationItem.REGISTERS, NavigationItem.CUSTOMERS,
                NavigationItem.LOYALTY, NavigationItem.DOCUMENTS)));
        assertFalse(allowed.contains(NavigationItem.SETTINGS));
        assertFalse(allowed.contains(NavigationItem.EMPLOYEES));
        assertFalse(allowed.contains(NavigationItem.AUDIT_LOG));
        assertEquals(NavigationItem.POS, policy.defaultItem(user("CASHIER")));
    }

    @Test void managerCannotAccessSensitiveAdministration() {
        AuthenticatedUser manager = user("MANAGER");
        var allowed = policy.allowedItems(manager);
        assertFalse(allowed.contains(NavigationItem.EMPLOYEES));
        assertFalse(allowed.contains(NavigationItem.AUDIT_LOG));
        assertFalse(allowed.contains(NavigationItem.SETTINGS));
        assertTrue(allowed.containsAll(Set.of(NavigationItem.POS, NavigationItem.INVENTORY,
                NavigationItem.PURCHASING, NavigationItem.REPORTS)));
        assertEquals(NavigationItem.DASHBOARD, policy.defaultItem(manager));
        assertTrue(policy.isManagerWorkspace(manager));
        assertFalse(policy.isCashierWorkspace(manager));
    }

    @Test void adminContinuesToStartOnDashboard() {
        assertEquals(NavigationItem.DASHBOARD, policy.defaultItem(user("ADMIN")));
    }

    @Test void elevatedRoleTakesPrecedenceOverCashierWorkspace() {
        AuthenticatedUser adminManagerCashier = user("ADMIN", "MANAGER", "CASHIER");
        assertEquals(NavigationItem.DASHBOARD, policy.defaultItem(adminManagerCashier));
        assertFalse(policy.isCashierWorkspace(adminManagerCashier));
        assertFalse(policy.isManagerWorkspace(adminManagerCashier));

        AuthenticatedUser managerCashier = user("MANAGER", "CASHIER");
        assertEquals(NavigationItem.DASHBOARD, policy.defaultItem(managerCashier));
        assertTrue(policy.isManagerWorkspace(managerCashier));
        assertFalse(policy.isCashierWorkspace(managerCashier));
    }

    @Test void rolelessAndUnknownUsersReceiveNoCashierAccess() {
        AuthenticatedUser roleless = user();
        AuthenticatedUser unknown = user("UNKNOWN");
        assertTrue(policy.allowedItems(roleless).isEmpty());
        assertTrue(policy.allowedItems(unknown).isEmpty());
        assertFalse(policy.isManagerWorkspace(roleless));
        assertFalse(policy.isManagerWorkspace(unknown));
        assertFalse(policy.isCashierWorkspace(roleless));
        assertFalse(policy.isCashierWorkspace(unknown));
        assertThrows(SecurityException.class, () -> policy.defaultItem(roleless));
        assertThrows(SecurityException.class, () -> policy.defaultItem(unknown));
    }

    @Test void accessGuardRejectsRestrictedModule() {
        assertThrows(SecurityException.class,
                () -> policy.requireAccess(user("CASHIER"), NavigationItem.SETTINGS));
    }

    private static AuthenticatedUser user(String... roles) {
        return new AuthenticatedUser(1, null, "user", "User", false,
                java.util.stream.IntStream.range(0, roles.length)
                        .mapToObj(index -> new RoleInfo(index + 1L, roles[index], roles[index]))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}
