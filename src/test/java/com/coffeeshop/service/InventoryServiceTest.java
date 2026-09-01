package com.coffeeshop.service;

import com.coffeeshop.dto.InventoryCommands;
import com.coffeeshop.model.InventoryModels;
import com.coffeeshop.repository.InventoryRepository;
import com.coffeeshop.validation.InventoryValidator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {
    private final Stub repository=new Stub();private final InventoryService service=new InventoryService(repository);
    @Test void validatesAndNormalizesInventoryItem(){service.saveItem(new InventoryCommands.Item(null,1," BEANS "," Coffee Beans ",value("5.5"),false,true,null));assertEquals("BEANS",repository.item.code());assertEquals("Coffee Beans",repository.item.name());assertEquals(value("5.5000"),repository.item.reorderLevel());}
    @Test void rejectsInvalidInventoryItem(){assertThrows(IllegalArgumentException.class,()->service.saveItem(new InventoryCommands.Item(null,0,"A","Item",BigDecimal.ZERO,false,true,null)));assertThrows(IllegalArgumentException.class,()->service.saveItem(new InventoryCommands.Item(null,1,"BAD CODE","Item",BigDecimal.ZERO,false,true,null)));}
    @Test void stockAdditionCreatesPositiveAdjustment(){service.addStock(3,7,value("2.5000"),"Delivery received",null);assertEquals(value("2.5000"),repository.adjustment.quantityDelta());assertEquals(7,repository.adjustment.userId());}
    @Test void stockDeductionCreatesNegativeAdjustment(){service.removeStock(3,7,value("1.2500"),"Manual usage",null);assertEquals(value("-1.2500"),repository.adjustment.quantityDelta());}
    @Test void rejectsZeroAndNegativeMovementMagnitude(){assertThrows(IllegalArgumentException.class,()->service.addStock(3,7,BigDecimal.ZERO,"Reason",null));assertThrows(IllegalArgumentException.class,()->service.removeStock(3,7,value("-1"),"Reason",null));}
    @Test void requiresAdjustmentReason(){assertThrows(IllegalArgumentException.class,()->service.addStock(3,7,BigDecimal.ONE," ",null));}
    @Test void preventsInsufficientStockByDefault(){assertThrows(IllegalArgumentException.class,()->InventoryValidator.resultingStock(value("1.0000"),value("-1.5000"),false));assertEquals(value("-0.5000"),InventoryValidator.resultingStock(value("1.0000"),value("-1.5000"),true));}
    @Test void validatesRecipeQuantityAndRelationships(){service.saveRecipe(new InventoryCommands.Recipe(null,11,22,value("0.0180"),null));assertEquals(11,repository.recipe.variantId());assertEquals(22,repository.recipe.inventoryItemId());assertThrows(IllegalArgumentException.class,()->service.saveRecipe(new InventoryCommands.Recipe(null,11,22,BigDecimal.ZERO,null)));}
    @Test void validatesModifierRecipeBehavior(){service.saveModifierRecipe(new InventoryCommands.ModifierRecipe(null,8,22,value("0.1500"),null));assertEquals(8,repository.modifierRecipe.modifierId());assertEquals(22,repository.modifierRecipe.inventoryItemId());assertThrows(IllegalArgumentException.class,()->service.saveModifierRecipe(new InventoryCommands.ModifierRecipe(null,8,22,value("-1"),null)));}
    @Test void recipeRemovalDelegatesOptimisticVersion(){byte[] version={1,2};service.deleteRecipe(new InventoryModels.RecipeItem(5,11,22,"Beans","Gram",value("1"),version));assertEquals(5,repository.deletedRecipe);assertArrayEquals(version,repository.deletedVersion);}

    private static BigDecimal value(String value){return new BigDecimal(value);}
    private static final class Stub implements InventoryRepository {
        InventoryCommands.Item item;InventoryCommands.Adjustment adjustment;InventoryCommands.Recipe recipe;InventoryCommands.ModifierRecipe modifierRecipe;long deletedRecipe;byte[] deletedVersion;
        public InventoryModels.Catalog loadCatalog(){return new InventoryModels.Catalog(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
        public long saveItem(InventoryCommands.Item command){item=command;return 1;}public void setItemActive(long id,boolean active,byte[] rowVersion){}
        public void recordAdjustment(InventoryCommands.Adjustment command){adjustment=command;}
        public long saveRecipe(InventoryCommands.Recipe command){recipe=command;return 1;}public void deleteRecipe(long id,byte[] rowVersion){deletedRecipe=id;deletedVersion=rowVersion;}
        public long saveModifierRecipe(InventoryCommands.ModifierRecipe command){modifierRecipe=command;return 1;}public void deleteModifierRecipe(long id,byte[] rowVersion){}
    }
}
