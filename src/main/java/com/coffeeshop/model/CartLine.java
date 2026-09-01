package com.coffeeshop.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
public final class CartLine {
    private final MenuVariant variant;
    private int quantity;
    public CartLine(MenuVariant variant) { this.variant=Objects.requireNonNull(variant); this.quantity=1; }
    public MenuVariant variant(){return variant;} public int quantity(){return quantity;}
    public void increment(){quantity++;} public void decrement(){if(quantity>1)quantity--;}
    public BigDecimal subtotal(){return variant.price().multiply(BigDecimal.valueOf(quantity)).setScale(4,RoundingMode.HALF_UP);}
    @Override public String toString(){return quantity+" × "+variant+"   "+subtotal().setScale(2,RoundingMode.HALF_UP);}
}
