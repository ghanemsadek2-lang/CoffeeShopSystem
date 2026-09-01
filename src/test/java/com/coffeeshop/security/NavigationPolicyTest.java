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
        assertTrue(allowed.containsAll(Set.of(NavigationItem.POS, NavigationItem.ORDERS, NavigationItem.TABLES)));
        assertFalse(allowed.contains(NavigationItem.SETTINGS));
        assertFalse(allowed.contains(NavigationItem.EMPLOYEES));
    }

    private static AuthenticatedUser user(String role) {
        return new AuthenticatedUser(1, null, "user", "User", false,
                Set.of(new RoleInfo(1, role, role)));
    }
}
