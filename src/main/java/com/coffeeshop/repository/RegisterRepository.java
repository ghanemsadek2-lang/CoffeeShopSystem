package com.coffeeshop.repository;

import com.coffeeshop.dto.RegisterCommands;
import com.coffeeshop.model.RegisterModels;

public interface RegisterRepository {
    RegisterModels.Dashboard loadDashboard(long userId,boolean includeAllHistory);
    long saveRegister(RegisterCommands.Register command);
    void setRegisterActive(long id,boolean active,byte[] rowVersion);
    long openShift(RegisterCommands.OpenShift command);
    long recordCashMovement(RegisterCommands.CashMovement command);
    void closeShift(RegisterCommands.CloseShift command);
}
