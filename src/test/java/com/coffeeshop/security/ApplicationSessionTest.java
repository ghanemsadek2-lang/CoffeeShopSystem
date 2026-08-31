package com.coffeeshop.security;

import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.RoleInfo;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationSessionTest {

    @Test
    void establishesAndClearsSession() {
        ApplicationSession session = new ApplicationSession();
        AuthenticatedUser user = user(1, "cashier");

        session.establish(user);

        assertTrue(session.isAuthenticated());
        assertEquals(user, session.currentUser().orElseThrow());

        session.clear();

        assertFalse(session.isAuthenticated());
        assertTrue(session.currentUser().isEmpty());
    }

    @Test
    void preventsReplacingAnActiveSession() {
        ApplicationSession session = new ApplicationSession();
        AuthenticatedUser first = user(1, "first");
        session.establish(first);

        assertThrows(IllegalStateException.class, () -> session.establish(user(2, "second")));
        assertEquals(first, session.currentUser().orElseThrow());
    }

    private static AuthenticatedUser user(long id, String username) {
        return new AuthenticatedUser(id, null, username, username, false,
                Set.of(new RoleInfo(1, "CASHIER", "Cashier")));
    }
}
