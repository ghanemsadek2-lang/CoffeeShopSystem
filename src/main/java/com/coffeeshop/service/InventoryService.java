package com.coffeeshop.service;

import com.coffeeshop.dto.InventoryCommands;
import com.coffeeshop.model.InventoryModels;
import com.coffeeshop.repository.InventoryRepository;
import com.coffeeshop.validation.InventoryValidator;
import java.math.BigDecimal;
import java.util.Objects;

/** Inventory rules; stock balance and ledger writes remain one repository transaction. */
public final class InventoryService {
    private final InventoryRepository repository;
    public InventoryService(InventoryRepository repository){this.repository=Objects.requireNonNull(repository);}
    public InventoryModels.Catalog catalog(){return repository.loadCatalog();}
    public long saveItem(InventoryCommands.Item value){return repository.saveItem(InventoryValidator.item(value));}
    public void setItemActive(InventoryModels.Item item,boolean active){Objects.requireNonNull(item);if(item.active()==active)throw new IllegalArgumentException("The inventory item already has that status.");repository.setItemActive(item.id(),active,item.rowVersion());}
    public void addStock(long itemId,long userId,BigDecimal quantity,String reason,String notes){repository.recordAdjustment(InventoryValidator.adjustment(new InventoryCommands.Adjustment(itemId,userId,positiveMagnitude(quantity,"Quantity"),reason,notes)));}
    public void removeStock(long itemId,long userId,BigDecimal quantity,String reason,String notes){repository.recordAdjustment(InventoryValidator.adjustment(new InventoryCommands.Adjustment(itemId,userId,positiveMagnitude(quantity,"Quantity").negate(),reason,notes)));}
    public long saveRecipe(InventoryCommands.Recipe value){return repository.saveRecipe(InventoryValidator.recipe(value));}
    public void deleteRecipe(InventoryModels.RecipeItem value){Objects.requireNonNull(value);repository.deleteRecipe(value.id(),value.rowVersion());}
    public long saveModifierRecipe(InventoryCommands.ModifierRecipe value){return repository.saveModifierRecipe(InventoryValidator.modifierRecipe(value));}
    public void deleteModifierRecipe(InventoryModels.ModifierRecipeItem value){Objects.requireNonNull(value);repository.deleteModifierRecipe(value.id(),value.rowVersion());}
    private static BigDecimal positiveMagnitude(BigDecimal value,String label){if(value==null||value.signum()<=0)throw new IllegalArgumentException(label+" must be greater than zero.");return value;}
}
