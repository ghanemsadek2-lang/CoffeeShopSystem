package com.coffeeshop.model;

import java.math.BigDecimal;

/** Active modifier that may be selected for a POS line. */
public record PosModifierOption(long modifierId, long groupId, String name,
                                BigDecimal priceAdjustment) { }
