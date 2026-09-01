package com.coffeeshop.validation;

import com.coffeeshop.dto.RegisterCommands;
import java.math.BigDecimal;

/** Schema-aligned normalization for register, shift, and cash writes. */
public final class RegisterValidator {
    private RegisterValidator() { }
    public static RegisterCommands.Register register(RegisterCommands.Register value){require(value!=null,"Register details are required.");return new RegisterCommands.Register(value.id(),code(value.code(),50,"Register code"),text(value.name(),100,"Register name"),order(value.displayOrder()),value.active(),version(value.id(),value.rowVersion()));}
    public static RegisterCommands.OpenShift open(RegisterCommands.OpenShift value){require(value!=null,"Shift details are required.");require(value.registerId()>0,"Select an active register.");require(value.userId()>0,"An authenticated user is required.");return new RegisterCommands.OpenShift(value.registerId(),value.userId(),money(value.openingCash(),true,"Opening cash"));}
    public static RegisterCommands.CashMovement movement(RegisterCommands.CashMovement value){require(value!=null,"Cash movement details are required.");require(value.shiftId()>0&&value.userId()>0,"An open shift and authenticated user are required.");require(value.type()!=null,"Select a cash movement type.");return new RegisterCommands.CashMovement(value.shiftId(),value.userId(),value.type(),money(value.amount(),false,"Amount"),text(value.reason(),500,"Reason"),optional(value.reference(),100,"Reference"),optional(value.notes(),1000,"Notes"));}
    public static RegisterCommands.CloseShift close(RegisterCommands.CloseShift value){require(value!=null,"Closing details are required.");require(value.shiftId()>0&&value.userId()>0,"An open shift and authenticated user are required.");require(value.rowVersion()!=null&&value.rowVersion().length>0,"Refresh the shift before closing it.");return new RegisterCommands.CloseShift(value.shiftId(),value.userId(),money(value.actualCash(),true,"Actual closing cash"),optional(value.varianceReason(),500,"Variance reason"),value.varianceAuthorized(),value.rowVersion());}
    private static BigDecimal money(BigDecimal value,boolean allowZero,String label){require(value!=null,label+" is required.");require(value.scale()<=4,label+" must have no more than four decimal places.");require(value.precision()-value.scale()<=15,label+" is too large.");require(allowZero?value.signum()>=0:value.signum()>0,label+(allowZero?" cannot be negative.":" must be greater than zero."));return value.setScale(4);}
    private static String code(String value,int max,String label){String normalized=text(value,max,label);require(!normalized.contains(" "),label+" must not contain spaces.");return normalized;}
    private static String text(String value,int max,String label){require(value!=null&&!value.isBlank(),label+" is required.");String normalized=value.trim();require(normalized.length()<=max,label+" is too long.");return normalized;}
    private static String optional(String value,int max,String label){if(value==null||value.isBlank())return null;String normalized=value.trim();require(normalized.length()<=max,label+" is too long.");return normalized;}
    private static int order(int value){require(value>=0,"Display order cannot be negative.");return value;}
    private static byte[] version(Long id,byte[] value){require(id==null||value!=null&&value.length>0,"Refresh the register before editing it.");return value;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
