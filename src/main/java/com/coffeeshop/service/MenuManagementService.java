package com.coffeeshop.service;

import com.coffeeshop.dto.MenuCommands;
import com.coffeeshop.model.MenuModels;
import com.coffeeshop.repository.MenuManagementRepository;
import com.coffeeshop.validation.MenuValidator;
import java.util.Objects;

/** Menu business rules and validated write orchestration. */
public final class MenuManagementService {
    private final MenuManagementRepository repository;
    public MenuManagementService(MenuManagementRepository repository){this.repository=Objects.requireNonNull(repository);}
    public MenuModels.Catalog catalog(){return repository.loadCatalog();}
    public long saveCategory(MenuCommands.Category value){return repository.saveCategory(MenuValidator.category(value));}
    public long saveProduct(MenuCommands.Product value){return repository.saveProduct(MenuValidator.product(value));}
    public long saveVariant(MenuCommands.Variant value){return repository.saveVariant(MenuValidator.variant(value));}
    public long saveGroup(MenuCommands.Group value){return repository.saveGroup(MenuValidator.group(value));}
    public long saveModifier(MenuCommands.Modifier value){return repository.saveModifier(MenuValidator.modifier(value));}
    public long saveAssignment(MenuCommands.Assignment value){return repository.saveAssignment(MenuValidator.assignment(value));}

    public void setProductActive(MenuModels.Product value,boolean active){requireChange(value.active(),active);repository.setProductActive(value.id(),active,value.rowVersion());}
    public void setCategoryActive(MenuModels.Category value,boolean active){requireChange(value.active(),active);repository.setCategoryActive(value.id(),active,value.rowVersion());}
    public void setVariantActive(MenuModels.Variant value,boolean active){requireChange(value.active(),active);repository.setVariantActive(value.id(),value.productId(),value.defaultVariant(),active,value.rowVersion());}
    public void setGroupActive(MenuModels.ModifierGroup value,boolean active){requireChange(value.active(),active);repository.setGroupActive(value.id(),active,value.rowVersion());}
    public void setModifierActive(MenuModels.Modifier value,boolean active){requireChange(value.active(),active);repository.setModifierActive(value.id(),active,value.rowVersion());}
    public void setAssignmentActive(MenuModels.Assignment value,boolean active){requireChange(value.active(),active);repository.setAssignmentActive(value.id(),active,value.rowVersion());}
    private static void requireChange(boolean current,boolean requested){if(current==requested)throw new IllegalArgumentException("The requested status is already set.");}
}
