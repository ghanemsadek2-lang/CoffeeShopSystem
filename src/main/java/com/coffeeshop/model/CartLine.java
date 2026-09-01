package com.coffeeshop.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public final class CartLine {
    private final MenuVariant variant;
    private final List<SelectedModifier> modifiers;
    private int quantity;

    public CartLine(MenuVariant variant) { this(variant,List.of()); }
    public CartLine(MenuVariant variant,List<SelectedModifier> modifiers) {
        this.variant=Objects.requireNonNull(variant);this.modifiers=List.copyOf(modifiers);this.quantity=1;
    }
    public MenuVariant variant(){return variant;}
    public List<SelectedModifier> modifiers(){return modifiers;}
    public int quantity(){return quantity;}
    public void increment(){quantity++;}
    public void decrement(){if(quantity>1)quantity--;}
    public BigDecimal unitPrice(){return modifiers.stream().map(SelectedModifier::priceAdjustment).reduce(variant.price(),BigDecimal::add).setScale(4,RoundingMode.HALF_UP);}
    public BigDecimal subtotal(){return unitPrice().multiply(BigDecimal.valueOf(quantity)).setScale(4,RoundingMode.HALF_UP);}
    public boolean sameSelection(MenuVariant candidate,List<SelectedModifier> selected){return variant.variantId()==candidate.variantId()&&modifiers.stream().map(SelectedModifier::modifierId).sorted().toList().equals(selected.stream().map(SelectedModifier::modifierId).sorted().toList());}
    @Override public String toString(){String choices=modifiers.isEmpty()?"":"\n   "+modifiers.stream().map(SelectedModifier::name).reduce((a,b)->a+", "+b).orElse("");return quantity+" × "+variant+choices+"   "+subtotal().setScale(2,RoundingMode.HALF_UP);}
}
