package com.coffeeshop.repository;

import com.coffeeshop.dto.LoyaltyCommands;
import com.coffeeshop.model.LoyaltyModels;

public interface LoyaltyRepository {
    LoyaltyModels.Catalog load();
    long enroll(LoyaltyCommands.Enroll command);
    long changePoints(LoyaltyCommands.ChangePoints command);
}
