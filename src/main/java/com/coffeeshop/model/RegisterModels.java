package com.coffeeshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read models for register configuration and cashier shift operations. */
public final class RegisterModels {
    private RegisterModels() { }
    public enum CashMovementType { CASH_IN, CASH_OUT }
    public record Register(long id,String code,String name,int displayOrder,boolean active,byte[] rowVersion) {
        @Override public String toString(){return name+" ("+code+")"+(active?"":" · inactive");}
    }
    public record Shift(long id,long registerId,String registerCode,String registerName,long openedByUserId,
                        String openedByUsername,LocalDateTime openedAt,LocalDateTime closedAt,
                        BigDecimal openingCash,BigDecimal expectedCash,BigDecimal actualCash,
                        BigDecimal difference,String status,byte[] rowVersion) { }
    public record CashMovement(long id,long shiftId,CashMovementType type,BigDecimal amount,String reason,
                               String reference,String notes,LocalDateTime movedAt,String recordedBy) { }
    public record Dashboard(List<Register> registers,Shift currentShift,List<Shift> shiftHistory,
                            List<CashMovement> currentMovements) {
        public Dashboard { registers=List.copyOf(registers);shiftHistory=List.copyOf(shiftHistory);currentMovements=List.copyOf(currentMovements); }
    }
}
