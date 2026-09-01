package com.coffeeshop.service;

import com.coffeeshop.dto.MenuCommands;
import com.coffeeshop.model.MenuModels;
import com.coffeeshop.repository.MenuManagementRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MenuManagementServiceTest {
    private final Stub repository=new Stub();
    private final MenuManagementService service=new MenuManagementService(repository);

    @Test void normalizesAndSavesCategory(){service.saveCategory(new MenuCommands.Category(null,"  Hot Drinks  ","  Espresso drinks  ",2,true,null));assertEquals("Hot Drinks",repository.category.name());assertEquals("Espresso drinks",repository.category.description());}
    @Test void rejectsBlankCategoryName(){assertThrows(IllegalArgumentException.class,()->service.saveCategory(new MenuCommands.Category(null," ",null,0,true,null)));}
    @Test void delegatesCategoryStatusChange(){MenuModels.Category category=new MenuModels.Category(5,"Tea",null,0,true,new byte[]{2});service.setCategoryActive(category,false);assertEquals(5,repository.categoryStatusId);assertFalse(repository.categoryStatus);}
    @Test void normalizesAndSavesProduct(){service.saveProduct(new MenuCommands.Product(null,1,null," LATTE "," Latte ","  milk  ","",0,true,null));assertEquals("LATTE",repository.product.code());assertEquals("Latte",repository.product.name());assertEquals("milk",repository.product.description());assertNull(repository.product.imagePath());}
    @Test void rejectsBlankProductName(){assertThrows(IllegalArgumentException.class,()->service.saveProduct(new MenuCommands.Product(null,1,null,"LATTE"," ",null,null,0,true,null)));}
    @Test void rejectsProductCodeWithSpaces(){assertThrows(IllegalArgumentException.class,()->service.saveProduct(new MenuCommands.Product(null,1,null,"HOT LATTE","Latte",null,null,0,true,null)));}
    @Test void rejectsNegativeVariantPrice(){assertThrows(IllegalArgumentException.class,()->service.saveVariant(new MenuCommands.Variant(null,1,null,"REG","Regular",null,new BigDecimal("-0.01"),0,false,true,null)));}
    @Test void rejectsOverPreciseVariantPrice(){assertThrows(IllegalArgumentException.class,()->service.saveVariant(new MenuCommands.Variant(null,1,null,"REG","Regular",null,new BigDecimal("1.00001"),0,false,true,null)));}
    @Test void savesValidVariant(){service.saveVariant(new MenuCommands.Variant(null,1,null,"REG","Regular",null,new BigDecimal("3.2500"),0,true,true,null));assertEquals(new BigDecimal("3.2500"),repository.variant.price());}
    @Test void rejectsInvalidModifierSelectionRange(){assertThrows(IllegalArgumentException.class,()->service.saveGroup(new MenuCommands.Group(null,"MILK","Milk",null,2,1,0,true,null)));}
    @Test void requiresBothAssignmentOverrides(){assertThrows(IllegalArgumentException.class,()->service.saveAssignment(new MenuCommands.Assignment(null,1,2,1,null,0,true,null)));}
    @Test void preventsNoOpStatusUpdate(){MenuModels.Product product=new MenuModels.Product(1,1,null,"P","Product",null,null,0,true,"Category",null,new byte[]{1});assertThrows(IllegalArgumentException.class,()->service.setProductActive(product,true));}
    @Test void delegatesDefaultVariantActivationContext(){MenuModels.Variant variant=new MenuModels.Variant(7,3,null,"D","Default",null,BigDecimal.ONE,0,true,false,null,new byte[]{4});service.setVariantActive(variant,true);assertEquals(7,repository.variantStatusId);assertEquals(3,repository.variantStatusProduct);assertTrue(repository.variantStatusDefault);assertTrue(repository.variantStatus);}

    private static final class Stub implements MenuManagementRepository {
        MenuCommands.Category category;MenuCommands.Product product;MenuCommands.Variant variant;long categoryStatusId,variantStatusId,variantStatusProduct;boolean categoryStatus,variantStatusDefault,variantStatus;
        public MenuModels.Catalog loadCatalog(){return new MenuModels.Catalog(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
        public long saveCategory(MenuCommands.Category value){category=value;return 1;}public void setCategoryActive(long id,boolean active,byte[] version){categoryStatusId=id;categoryStatus=active;}
        public long saveProduct(MenuCommands.Product value){product=value;return 1;}public void setProductActive(long id,boolean active,byte[] version){}
        public long saveVariant(MenuCommands.Variant value){variant=value;return 1;}public void setVariantActive(long id,long productId,boolean defaultVariant,boolean active,byte[] version){variantStatusId=id;variantStatusProduct=productId;variantStatusDefault=defaultVariant;variantStatus=active;}
        public long saveGroup(MenuCommands.Group value){return 1;}public void setGroupActive(long id,boolean active,byte[] version){}
        public long saveModifier(MenuCommands.Modifier value){return 1;}public void setModifierActive(long id,boolean active,byte[] version){}
        public long saveAssignment(MenuCommands.Assignment value){return 1;}public void setAssignmentActive(long id,boolean active,byte[] version){}
    }
}
