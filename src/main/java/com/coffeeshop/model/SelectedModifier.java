package com.coffeeshop.model;

import java.math.BigDecimal;

/** Immutable modifier snapshot carried by a cart line into order persistence. */
public record SelectedModifier(long modifierId, long groupId, String name,
                               BigDecimal priceAdjustment) { }
