package com.coffeeshop.service;

import com.coffeeshop.dto.RegisterCommands;
import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.RegisterModels;
import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.repository.RegisterRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterServiceTest {
    private final Stub repository = new Stub();
    private final RegisterService service = new RegisterService(repository);

    @Test void validatesAndNormalizesRegister() {
        service.saveRegister(user("MANAGER"), new RegisterCommands.Register(null, " POS-1 ", " Front Register ", 2, true, null));
        assertEquals("POS-1", repository.register.code());
        assertEquals("Front Register", repository.register.name());
    }

    @Test void rejectsInvalidRegisterAndCashValues() {
        assertThrows(IllegalArgumentException.class, () -> service.saveRegister(user("MANAGER"),
                new RegisterCommands.Register(null, "BAD CODE", "Register", 0, true, null)));
        assertThrows(IllegalArgumentException.class, () -> service.openShift(user("CASHIER"), 1, new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> service.recordMovement(user("MANAGER"), 1,
                RegisterModels.CashMovementType.CASH_IN, BigDecimal.ZERO, "Reason", null, null));
    }

    @Test void registerActivationRequiresManagerAndUsesOptimisticVersion() {
        RegisterModels.Register register = register(true);
        assertThrows(SecurityException.class, () -> service.setRegisterActive(user("CASHIER"), register, false));
        service.setRegisterActive(user("MANAGER"), register, false);
        assertEquals(register.id(), repository.statusId);
        assertArrayEquals(register.rowVersion(), repository.statusVersion);
    }

    @Test void opensShiftForAuthenticatedCashier() {
        service.openShift(user("CASHIER"), 7, value("100"));
        assertEquals(7, repository.open.registerId());
        assertEquals(11, repository.open.userId());
        assertEquals(value("100.0000"), repository.open.openingCash());
    }

    @Test void duplicateOpenShiftFailureIsPreserved() {
        repository.openFailure = new IllegalStateException("You already have an open register shift.");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.openShift(user("CASHIER"), 7, BigDecimal.ZERO));
        assertEquals("You already have an open register shift.", error.getMessage());
    }

    @Test void recordsCashInAndCashOutWithRequiredAuditFields() {
        service.recordMovement(user("MANAGER"), 9, RegisterModels.CashMovementType.CASH_IN,
                value("20"), "Float increase", "REF-1", null);
        assertEquals(RegisterModels.CashMovementType.CASH_IN, repository.movement.type());
        service.recordMovement(user("MANAGER"), 9, RegisterModels.CashMovementType.CASH_OUT,
                value("5"), "Safe drop", null, null);
        assertEquals(value("5.0000"), repository.movement.amount());
    }

    @Test void rejectsMissingMovementReasonAndCashierManualMovement() {
        assertThrows(IllegalArgumentException.class, () -> service.recordMovement(user("MANAGER"), 9,
                RegisterModels.CashMovementType.CASH_IN, BigDecimal.ONE, " ", null, null));
        assertThrows(SecurityException.class, () -> service.recordMovement(user("CASHIER"), 9,
                RegisterModels.CashMovementType.CASH_OUT, BigDecimal.ONE, "Safe drop", null, null));
    }

    @Test void closedShiftMovementFailureIsPreserved() {
        repository.movementFailure = new IllegalStateException("The register shift is no longer open.");
        assertThrows(IllegalStateException.class, () -> service.recordMovement(user("MANAGER"), 9,
                RegisterModels.CashMovementType.CASH_IN, BigDecimal.ONE, "Reason", null, null));
    }

    @Test void calculatesExpectedCashFromSupportedSources() {
        assertEquals(value("147.5000"), RegisterService.expectedCash(value("100"), value("42.5"),
                value("10"), value("5")));
    }

    @Test void closesBalancedShiftAndPassesRowVersion() {
        RegisterModels.Shift shift = shift(value("125"));
        service.closeShift(user("MANAGER"), shift, value("125"), null);
        assertEquals(value("125.0000"), repository.close.actualCash());
        assertArrayEquals(shift.rowVersion(), repository.close.rowVersion());
    }

    @Test void cashierCannotCloseShiftWithVariance() {
        assertThrows(SecurityException.class, () -> service.closeShift(user("CASHIER"), shift(value("125")), value("124"), "Short"));
    }

    @Test void managerVarianceRequiresReasonAndIsAuthorized() {
        assertThrows(IllegalArgumentException.class, () -> service.closeShift(user("MANAGER"), shift(value("125")), value("124"), " "));
        service.closeShift(user("MANAGER"), shift(value("125")), value("124"), "Till was short");
        assertTrue(repository.close.varianceAuthorized());
        assertEquals("Till was short", repository.close.varianceReason());
    }

    @Test void staleShiftFailureIsPreserved() {
        repository.closeFailure = new IllegalStateException("The register shift changed. Refresh and try again.");
        assertThrows(IllegalStateException.class, () -> service.closeShift(user("MANAGER"), shift(value("125")), value("125"), null));
    }

    private static AuthenticatedUser user(String role) {
        return new AuthenticatedUser(11, null, "cashier", "Cashier", false,
                Set.of(new RoleInfo(1, role, role)));
    }

    private static RegisterModels.Register register(boolean active) {
        return new RegisterModels.Register(7, "POS-1", "Front Register", 1, active, new byte[]{1, 2});
    }

    private static RegisterModels.Shift shift(BigDecimal expected) {
        return new RegisterModels.Shift(9, 7, "POS-1", "Front Register", 11, "cashier",
                LocalDateTime.of(2026, 1, 1, 8, 0), null, value("100"), expected,
                null, null, "OPEN", new byte[]{3, 4});
    }

    private static BigDecimal value(String value) { return new BigDecimal(value); }

    private static final class Stub implements RegisterRepository {
        RegisterCommands.Register register;
        RegisterCommands.OpenShift open;
        RegisterCommands.CashMovement movement;
        RegisterCommands.CloseShift close;
        long statusId;
        byte[] statusVersion;
        RuntimeException openFailure;
        RuntimeException movementFailure;
        RuntimeException closeFailure;

        @Override public RegisterModels.Dashboard loadDashboard(long userId, boolean includeAllHistory) {
            return new RegisterModels.Dashboard(List.of(), null, List.of(), List.of());
        }
        @Override public long saveRegister(RegisterCommands.Register command) { register = command; return 1; }
        @Override public void setRegisterActive(long id, boolean active, byte[] rowVersion) { statusId = id; statusVersion = rowVersion; }
        @Override public long openShift(RegisterCommands.OpenShift command) { if (openFailure != null) throw openFailure; open = command; return 1; }
        @Override public long recordCashMovement(RegisterCommands.CashMovement command) { if (movementFailure != null) throw movementFailure; movement = command; return 1; }
        @Override public void closeShift(RegisterCommands.CloseShift command) { if (closeFailure != null) throw closeFailure; close = command; }
    }
}
