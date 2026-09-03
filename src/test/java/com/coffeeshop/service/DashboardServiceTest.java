package com.coffeeshop.service;

import com.coffeeshop.model.DashboardModels;
import com.coffeeshop.repository.DashboardRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class DashboardServiceTest {
    @Test
    void returnsRepositorySnapshot() {
        DashboardModels.Snapshot expected = new DashboardModels.Snapshot(
                new DashboardModels.Summary(new BigDecimal("12.5000"), 3, 2, 1), List.of());
        DashboardRepository repository = () -> expected;

        assertSame(expected, new DashboardService(repository).snapshot());
    }
}
