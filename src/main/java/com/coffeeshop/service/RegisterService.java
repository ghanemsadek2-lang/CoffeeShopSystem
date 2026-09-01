package com.coffeeshop.service;

import com.coffeeshop.dto.RegisterCommands;
import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.RegisterModels;
import com.coffeeshop.repository.RegisterRepository;
import com.coffeeshop.validation.RegisterValidator;
import java.math.BigDecimal;
import java.util.Objects;

/** Role-aware register and shift rules over transactional repository operations. */
public final class RegisterService {
    private final RegisterRepository repository;
    public RegisterService(RegisterRepository repository){this.repository=Objects.requireNonNull(repository);}
    public RegisterModels.Dashboard dashboard(AuthenticatedUser user){Objects.requireNonNull(user);return repository.loadDashboard(user.userId(),canManage(user));}
    public long saveRegister(AuthenticatedUser user,RegisterCommands.Register value){requireManager(user,"Only managers and administrators can configure registers.");return repository.saveRegister(RegisterValidator.register(value));}
    public void setRegisterActive(AuthenticatedUser user,RegisterModels.Register register,boolean active){requireManager(user,"Only managers and administrators can configure registers.");Objects.requireNonNull(register);if(register.active()==active)throw new IllegalArgumentException("The register already has that status.");repository.setRegisterActive(register.id(),active,register.rowVersion());}
    public long openShift(AuthenticatedUser user,long registerId,BigDecimal openingCash){Objects.requireNonNull(user);return repository.openShift(RegisterValidator.open(new RegisterCommands.OpenShift(registerId,user.userId(),openingCash)));}
    public long recordMovement(AuthenticatedUser user,long shiftId,RegisterModels.CashMovementType type,BigDecimal amount,String reason,String reference,String notes){requireManager(user,"Only managers and administrators can record manual cash movements.");return repository.recordCashMovement(RegisterValidator.movement(new RegisterCommands.CashMovement(shiftId,user.userId(),type,amount,reason,reference,notes)));}
    public void closeShift(AuthenticatedUser user,RegisterModels.Shift shift,BigDecimal actualCash,String varianceReason){
        Objects.requireNonNull(user);
        Objects.requireNonNull(shift);
        if(shift.openedByUserId()!=user.userId())throw new IllegalStateException("Only the user who opened this shift can close it.");
        RegisterCommands.CloseShift command=RegisterValidator.close(new RegisterCommands.CloseShift(
                shift.id(),user.userId(),actualCash,varianceReason,canManage(user),shift.rowVersion()));
        BigDecimal variance=command.actualCash().subtract(shift.expectedCash());
        if(variance.signum()!=0&&!command.varianceAuthorized())throw new SecurityException("A manager or administrator must close a shift with a cash variance.");
        if(variance.signum()!=0&&command.varianceReason()==null)throw new IllegalArgumentException("A reason is required for a cash variance.");
        repository.closeShift(command);
    }
    public static BigDecimal expectedCash(BigDecimal openingCash,BigDecimal cashPayments,BigDecimal cashIn,BigDecimal cashOut){
        return Objects.requireNonNull(openingCash).add(Objects.requireNonNull(cashPayments))
                .add(Objects.requireNonNull(cashIn)).subtract(Objects.requireNonNull(cashOut)).setScale(4);
    }
    public boolean canManage(AuthenticatedUser user){return user.hasRole("ADMIN")||user.hasRole("MANAGER");}
    private static void requireManager(AuthenticatedUser user,String message){Objects.requireNonNull(user);if(!user.hasRole("ADMIN")&&!user.hasRole("MANAGER"))throw new SecurityException(message);}
}
