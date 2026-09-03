package com.coffeeshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class DashboardModels {
    private DashboardModels() {
    }

    public record Summary(BigDecimal todaySales, long todayOrders, long activeTables, long lowStockItems) {
    }

    public record Activity(String orderNumber, String orderType, String status,
                           BigDecimal total, LocalDateTime occurredAt) {
    }

    public record Snapshot(Summary summary, List<Activity> recentActivity) {
        public Snapshot {
            recentActivity = List.copyOf(recentActivity);
        }
    }
}
