package com.coffeeshop.model;
import java.util.List;
public record OrderDetails(OrderSummary order,List<String> lineDescriptions){public OrderDetails{lineDescriptions=List.copyOf(lineDescriptions);}}
