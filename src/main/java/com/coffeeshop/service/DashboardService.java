package com.coffeeshop.service;

import com.coffeeshop.model.DashboardModels;
import com.coffeeshop.repository.DashboardRepository;

import java.util.Objects;

public final class DashboardService {
    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public DashboardModels.Snapshot snapshot() {
        return repository.loadSnapshot();
    }
}
