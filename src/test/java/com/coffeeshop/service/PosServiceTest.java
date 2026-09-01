package com.coffeeshop.service;

import com.coffeeshop.dto.*;
import com.coffeeshop.model.*;
import com.coffeeshop.repository.PosRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PosServiceTest {
    private final Stub repo=new Stub();
    private final PosService service=new PosService(repo);
    private final MenuVariant variant=new MenuVariant(1,1,1,"Latte","Regular",money("3.2500"),null,null,null);
    private final PosModifierOption oat=option(10,2,"Oat milk","0.7500");
    private final PosModifierOption almond=option(11,2,"Almond milk","0.5000");

    @Test void noModifierProductReturnsNoGroups(){assertTrue(service.modifierGroups(1).isEmpty());}
    @Test void optionalGroupAllowsNoSelection(){assertTrue(service.validateModifierSelection(List.of(group(0,1,oat)),List.of()).isEmpty());}
    @Test void requiredGroupRejectsNoSelection(){assertThrows(IllegalArgumentException.class,()->service.validateModifierSelection(List.of(group(1,1,oat)),List.of()));}
    @Test void rejectsMoreThanMaximum(){assertThrows(IllegalArgumentException.class,()->service.validateModifierSelection(List.of(group(0,1,oat,almond)),List.of(oat,almond)));}
    @Test void effectiveAssignmentOverridesAreEnforced(){PosModifierGroup override=group(2,2,oat,almond);assertThrows(IllegalArgumentException.class,()->service.validateModifierSelection(List.of(override),List.of(oat)));assertEquals(2,service.validateModifierSelection(List.of(override),List.of(oat,almond)).size());}
    @Test void calculatesModifierAdjustedUnitPrice(){CartLine line=new CartLine(variant,service.validateModifierSelection(List.of(group(0,1,oat)),List.of(oat)));assertEquals(money("4.0000"),line.unitPrice());}
    @Test void permitsMultipleModifiersWhenAllowed(){assertEquals(2,service.validateModifierSelection(List.of(group(0,2,oat,almond)),List.of(oat,almond)).size());}
    @Test void rejectsInactiveOrUnassignedModifier(){PosModifierOption inactive=option(99,2,"Inactive","1.0000");assertThrows(IllegalArgumentException.class,()->service.validateModifierSelection(List.of(group(0,1,oat)),List.of(inactive)));}
    @Test void quantityUsesAdjustedPriceForLineTotal(){CartLine line=new CartLine(variant,List.of(selected(oat)));line.increment();assertEquals(money("8.0000"),line.subtotal());assertEquals(money("8.0000"),service.subtotal(List.of(line)));}
    @Test void cartLinesMergeOnlyForIdenticalSelections(){CartLine oatLine=new CartLine(variant,List.of(selected(oat)));assertTrue(oatLine.sameSelection(variant,List.of(selected(oat))));assertFalse(oatLine.sameSelection(variant,List.of(selected(almond))));}
    @Test void selectedModifierRetainsPersistenceSnapshot(){SelectedModifier selected=service.validateModifierSelection(List.of(group(0,1,oat)),List.of(oat)).getFirst();assertEquals(oat.modifierId(),selected.modifierId());assertEquals(oat.name(),selected.name());assertEquals(oat.priceAdjustment(),selected.priceAdjustment());}
    @Test void requiresItems(){assertThrows(IllegalArgumentException.class,()->service.saveOpenOrder(new CreateOrderRequest(1,OrderType.TAKEAWAY,null,null,List.of())));}
    @Test void requiresTableForDineIn(){assertThrows(IllegalArgumentException.class,()->service.saveOpenOrder(new CreateOrderRequest(1,OrderType.DINE_IN,null,null,List.of(new CartLine(variant)))));}
    @Test void requiresCompleteDeliveryData(){assertThrows(IllegalArgumentException.class,()->service.saveOpenOrder(new CreateOrderRequest(1,OrderType.DELIVERY,null,new DeliveryInfo("Name","","Address","City"),List.of(new CartLine(variant)))));}
    @Test void validTakeawayPersists(){var q=new CreateOrderRequest(1,OrderType.TAKEAWAY,null,null,List.of(new CartLine(variant)));assertEquals("ORD00000001",service.saveOpenOrder(q));assertEquals(q,repo.saved);}

    private static PosModifierGroup group(int min,int max,PosModifierOption... options){return new PosModifierGroup(1,2,"Milk Choice",min,max,List.of(options));}
    private static PosModifierOption option(long id,long group,String name,String price){return new PosModifierOption(id,group,name,money(price));}
    private static SelectedModifier selected(PosModifierOption option){return new SelectedModifier(option.modifierId(),option.groupId(),option.name(),option.priceAdjustment());}
    private static BigDecimal money(String value){return new BigDecimal(value);}

    private static final class Stub implements PosRepository {
        CreateOrderRequest saved;
        public List<MenuCategory>findActiveCategories(){return List.of();}
        public List<MenuVariant>findActiveMenuVariants(){return List.of();}
        public List<PosModifierGroup>findActiveModifierGroups(long productId){return List.of();}
        public List<CafeTableInfo>findAvailableTables(){return List.of();}
        public String createOpenOrder(CreateOrderRequest q){saved=q;return "ORD00000001";}
    }
}
