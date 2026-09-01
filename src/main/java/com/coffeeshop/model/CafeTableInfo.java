package com.coffeeshop.model;
public record CafeTableInfo(long id, String code, String name, int capacity, String status) {
    @Override public String toString() { return name + " (" + capacity + ")"; }
}
