package com.coffeeshop.repository;

import com.coffeeshop.dto.InventoryCommands;
import com.coffeeshop.model.InventoryModels;

public interface InventoryRepository {
    InventoryModels.Catalog loadCatalog();
    long saveItem(InventoryCommands.Item command);
    void setItemActive(long id,boolean active,byte[] rowVersion);
    void recordAdjustment(InventoryCommands.Adjustment command);
    long saveRecipe(InventoryCommands.Recipe command);
    void deleteRecipe(long id,byte[] rowVersion);
    long saveModifierRecipe(InventoryCommands.ModifierRecipe command);
    void deleteModifierRecipe(long id,byte[] rowVersion);
}
