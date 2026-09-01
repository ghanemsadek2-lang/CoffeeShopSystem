package com.coffeeshop.repository;

import com.coffeeshop.dto.MenuCommands;
import com.coffeeshop.model.MenuModels;

public interface MenuManagementRepository {
    MenuModels.Catalog loadCatalog();
    long saveCategory(MenuCommands.Category command);
    void setCategoryActive(long id, boolean active, byte[] rowVersion);
    long saveProduct(MenuCommands.Product command);
    void setProductActive(long id, boolean active, byte[] rowVersion);
    long saveVariant(MenuCommands.Variant command);
    void setVariantActive(long id, long productId, boolean defaultVariant, boolean active, byte[] rowVersion);
    long saveGroup(MenuCommands.Group command);
    void setGroupActive(long id, boolean active, byte[] rowVersion);
    long saveModifier(MenuCommands.Modifier command);
    void setModifierActive(long id, boolean active, byte[] rowVersion);
    long saveAssignment(MenuCommands.Assignment command);
    void setAssignmentActive(long id, boolean active, byte[] rowVersion);
}
