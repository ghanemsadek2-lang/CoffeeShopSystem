package com.coffeeshop.dto;

import com.coffeeshop.model.RegisterModels.CashMovementType;
import java.math.BigDecimal;

/** Register and shift write requests validated before persistence. */
public final class RegisterCommands {
    private RegisterCommands() { }
    public record Register(Long id,String code,String name,int displayOrder,boolean active,byte[] rowVersion) { }
    public record OpenShift(long registerId,long userId,BigDecimal openingCash) { }
    public record CashMovement(long shiftId,long userId,CashMovementType type,BigDecimal amount,
                               String reason,String reference,String notes) { }
    public record CloseShift(long shiftId,long userId,BigDecimal actualCash,String varianceReason,
                             boolean varianceAuthorized,byte[] rowVersion) { }
}
