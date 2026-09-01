package com.coffeeshop.validation;

import com.coffeeshop.dto.InventoryCommands;
import java.math.BigDecimal;

/** Schema-aligned validation and normalization for inventory writes. */
public final class InventoryValidator {
    private InventoryValidator() { }

    public static InventoryCommands.Item item(InventoryCommands.Item value){
        require(value!=null,"Inventory item details are required.");require(value.unitId()>0,"Select a unit of measure.");
        return new InventoryCommands.Item(value.id(),value.unitId(),code(value.code(),50,"Item code"),text(value.name(),150,"Item name"),quantity(value.reorderLevel(),true,"Reorder level"),value.allowNegative(),value.active(),version(value.id(),value.rowVersion()));
    }
    public static InventoryCommands.Adjustment adjustment(InventoryCommands.Adjustment value){
        require(value!=null,"Stock adjustment details are required.");require(value.itemId()>0,"Select an inventory item.");require(value.userId()>0,"An authenticated user is required.");BigDecimal delta=quantity(value.quantityDelta(),false,"Quantity");require(delta.signum()!=0,"Quantity must be greater than zero.");return new InventoryCommands.Adjustment(value.itemId(),value.userId(),delta,text(value.reason(),500,"Reason"),optional(value.notes(),1000,"Notes"));
    }
    public static InventoryCommands.Recipe recipe(InventoryCommands.Recipe value){
        require(value!=null,"Recipe details are required.");require(value.variantId()>0&&value.inventoryItemId()>0,"Select a variant and inventory item.");return new InventoryCommands.Recipe(value.id(),value.variantId(),value.inventoryItemId(),positive(value.quantityRequired()),version(value.id(),value.rowVersion()));
    }
    public static InventoryCommands.ModifierRecipe modifierRecipe(InventoryCommands.ModifierRecipe value){
        require(value!=null,"Modifier recipe details are required.");require(value.modifierId()>0&&value.inventoryItemId()>0,"Select a modifier and inventory item.");return new InventoryCommands.ModifierRecipe(value.id(),value.modifierId(),value.inventoryItemId(),positive(value.quantityRequired()),version(value.id(),value.rowVersion()));
    }
    public static BigDecimal resultingStock(BigDecimal current,BigDecimal delta,boolean allowNegative){require(current!=null&&delta!=null,"Stock quantities are required.");BigDecimal result=current.add(delta).setScale(4);require(allowNegative||result.signum()>=0,"This adjustment would produce negative stock.");return result;}
    private static BigDecimal positive(BigDecimal value){BigDecimal q=quantity(value,false,"Quantity required");require(q.signum()>0,"Quantity required must be greater than zero.");return q;}
    private static BigDecimal quantity(BigDecimal value,boolean allowZero,String label){require(value!=null,label+" is required.");require(value.scale()<=4,label+" must have no more than four decimal places.");require(value.precision()-value.scale()<=15,label+" is too large.");if(!allowZero)require(value.signum()!=0,label+" must not be zero.");else require(value.signum()>=0,label+" cannot be negative.");return value.setScale(4);}
    private static String code(String value,int max,String label){String normalized=text(value,max,label);require(!normalized.contains(" "),label+" must not contain spaces.");return normalized;}
    private static String text(String value,int max,String label){require(value!=null&&!value.isBlank(),label+" is required.");String normalized=value.trim();require(normalized.length()<=max,label+" is too long.");return normalized;}
    private static String optional(String value,int max,String label){if(value==null||value.isBlank())return null;String normalized=value.trim();require(normalized.length()<=max,label+" is too long.");return normalized;}
    private static byte[] version(Long id,byte[] value){require(id==null||(value!=null&&value.length>0),"This record must be refreshed before editing.");return value;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
