package com.coffeeshop.service;

import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.security.UserCredentials;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceTest {

    @Test
    void rejectsMissingInputWithoutRepositoryLookup() {
        StubRepository repository = new StubRepository(Optional.empty());
        AuthenticationService service = new AuthenticationService(repository, (password, hash) -> false);
        char[] suppliedPassword = "password".toCharArray();

        LoginResult blankUsername = service.login("  ", suppliedPassword);
        LoginResult blankPassword = service.login("cashier", new char[0]);

        assertEquals(LoginStatus.INVALID_INPUT, blankUsername.status());
        assertEquals(LoginStatus.INVALID_INPUT, blankPassword.status());
        assertEquals(0, repository.userLookups);
        assertCleared(suppliedPassword);
    }

    @Test
    void unknownUserAndWrongPasswordUseSameGenericResult() {
        AuthenticationService unknownUserService = new AuthenticationService(
                new StubRepository(Optional.empty()), (password, hash) -> false);
        AuthenticationService wrongPasswordService = new AuthenticationService(
                new StubRepository(Optional.of(credentials())), (password, hash) -> false);

        char[] unknownPassword = "password".toCharArray();
        char[] wrongPassword = "wrong".toCharArray();
        LoginResult unknown = unknownUserService.login("missing", unknownPassword);
        LoginResult wrong = wrongPasswordService.login("cashier", wrongPassword);

        assertEquals(LoginStatus.INVALID_CREDENTIALS, unknown.status());
        assertEquals(LoginStatus.INVALID_CREDENTIALS, wrong.status());
        assertEquals(unknown.message(), wrong.message());
        assertTrue(unknown.user().isEmpty());
        assertTrue(wrong.user().isEmpty());
        assertCleared(unknownPassword);
        assertCleared(wrongPassword);
    }

    @Test
    void successfulLoginReturnsSafeIdentityAndActiveRoles() {
        StubRepository repository = new StubRepository(Optional.of(credentials()));
        AuthenticationService service = new AuthenticationService(repository, (password, hash) -> true);
        char[] suppliedPassword = "password".toCharArray();

        LoginResult result = service.login(" cashier ", suppliedPassword);

        assertTrue(result.successful());
        assertEquals("cashier", result.user().orElseThrow().username());
        assertTrue(result.user().orElseThrow().hasRole("CASHIER"));
        assertEquals(1, repository.roleLookups);
        assertCleared(suppliedPassword);
    }

    @Test
    void clearsPasswordWhenAccountIsLocked() {
        UserCredentials lockedCredentials = new UserCredentials(7, 3L, "cashier", "Casey Cashier",
                "$2a$04$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu",
                false, Instant.now().plusSeconds(300));
        AuthenticationService service = new AuthenticationService(
                new StubRepository(Optional.of(lockedCredentials)), (password, hash) -> true);
        char[] suppliedPassword = "password".toCharArray();

        LoginResult result = service.login("cashier", suppliedPassword);

        assertEquals(LoginStatus.ACCOUNT_LOCKED, result.status());
        assertCleared(suppliedPassword);
    }

    @Test
    void clearsPasswordWhenUserLookupFails() {
        UserRepository repository = new FailingRepository(true);
        AuthenticationService service = new AuthenticationService(repository, (password, hash) -> true);
        char[] suppliedPassword = "password".toCharArray();

        assertThrows(DatabaseException.class, () -> service.login("cashier", suppliedPassword));

        assertCleared(suppliedPassword);
    }

    @Test
    void clearsPasswordWhenRoleLoadingFails() {
        UserRepository repository = new FailingRepository(false);
        AuthenticationService service = new AuthenticationService(repository, (password, hash) -> true);
        char[] suppliedPassword = "password".toCharArray();

        assertThrows(DatabaseException.class, () -> service.login("cashier", suppliedPassword));

        assertCleared(suppliedPassword);
    }

    private static UserCredentials credentials() {
        return new UserCredentials(7, 3L, "cashier", "Casey Cashier",
                "$2a$04$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu",
                false, null);
    }

    private static void assertCleared(char[] password) {
        assertArrayEquals(new char[password.length], password);
    }

    private static final class FailingRepository implements UserRepository {
        private final boolean failUserLookup;

        private FailingRepository(boolean failUserLookup) {
            this.failUserLookup = failUserLookup;
        }

        @Override
        public Optional<UserCredentials> findActiveByUsername(String username) {
            if (failUserLookup) {
                throw new DatabaseException("Test user lookup failure.");
            }
            return Optional.of(credentials());
        }

        @Override
        public Set<RoleInfo> findActiveRoles(long userId) {
            throw new DatabaseException("Test role lookup failure.");
        }
    }

    private static final class StubRepository implements UserRepository {
        private final Optional<UserCredentials> user;
        private int userLookups;
        private int roleLookups;

        private StubRepository(Optional<UserCredentials> user) {
            this.user = user;
        }

        @Override
        public Optional<UserCredentials> findActiveByUsername(String username) {
            userLookups++;
            return user;
        }

        @Override
        public Set<RoleInfo> findActiveRoles(long userId) {
            roleLookups++;
            return Set.of(new RoleInfo(2, "CASHIER", "Cashier"));
        }
    }
}
