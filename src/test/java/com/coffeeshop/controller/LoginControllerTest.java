package com.coffeeshop.controller;

import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.repository.UserRepository;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.service.AuthenticationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginControllerTest {

    @Test
    void closesItsReusableAuthenticationExecutor() {
        RecordingExecutor executor = new RecordingExecutor();
        LoginController controller = new LoginController(authenticationService(), new ApplicationSession(), user -> {}, executor);

        controller.close();

        assertTrue(executor.shutdownNowCalled);
    }

    @Test
    void rejectsMissingInjectedDependencies() {
        RecordingExecutor executor = new RecordingExecutor();

        assertThrows(NullPointerException.class,
                () -> new LoginController(null, new ApplicationSession(), user -> {}, executor));
        assertThrows(NullPointerException.class,
                () -> new LoginController(authenticationService(), null, user -> {}, executor));
        assertThrows(NullPointerException.class,
                () -> new LoginController(authenticationService(), new ApplicationSession(), user -> {}, null));
    }

    private static AuthenticationService authenticationService() {
        UserRepository repository = new UserRepository() {
            @Override
            public Optional<com.coffeeshop.security.UserCredentials> findActiveByUsername(String username) {
                return Optional.empty();
            }

            @Override
            public Set<RoleInfo> findActiveRoles(long userId) {
                return Set.of();
            }
        };
        return new AuthenticationService(repository, (password, hash) -> false);
    }

    private static final class RecordingExecutor extends AbstractExecutorService {
        private boolean shutdownNowCalled;

        @Override
        public void shutdown() {
            shutdownNowCalled = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdownNowCalled = true;
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdownNowCalled;
        }

        @Override
        public boolean isTerminated() {
            return shutdownNowCalled;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdownNowCalled;
        }

        @Override
        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }
    }
}
