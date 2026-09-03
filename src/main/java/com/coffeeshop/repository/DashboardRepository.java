package com.coffeeshop.repository;

import com.coffeeshop.model.DashboardModels;

public interface DashboardRepository {
    DashboardModels.Snapshot loadSnapshot();
}
