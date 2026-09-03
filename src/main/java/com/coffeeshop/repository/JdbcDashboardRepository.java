package com.coffeeshop.repository;

import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.DashboardModels;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcDashboardRepository implements DashboardRepository {
    private static final String SUMMARY_SQL = """
            SELECT
                COALESCE(SUM(CASE WHEN o.status = 'COMPLETED'
                    AND o.completed_at >= CONVERT(date, SYSUTCDATETIME())
                    AND o.completed_at < DATEADD(day, 1, CONVERT(date, SYSUTCDATETIME()))
                    THEN o.total_amount ELSE 0 END), 0) AS today_sales,
                SUM(CASE WHEN o.opened_at >= CONVERT(date, SYSUTCDATETIME())
                    AND o.opened_at < DATEADD(day, 1, CONVERT(date, SYSUTCDATETIME()))
                    THEN 1 ELSE 0 END) AS today_orders,
                (SELECT COUNT_BIG(*) FROM dbo.cafe_tables
                    WHERE is_active = 1 AND status IN ('OCCUPIED', 'RESERVED')) AS active_tables,
                (SELECT COUNT_BIG(*) FROM dbo.inventory_items
                    WHERE is_active = 1 AND current_stock_quantity <= reorder_level) AS low_stock_items
            FROM dbo.orders o
            """;

    private static final String ACTIVITY_SQL = """
            SELECT TOP (8) order_number, order_type, status, total_amount,
                COALESCE(completed_at, opened_at) AS occurred_at
            FROM dbo.orders
            WHERE status IN ('OPEN', 'COMPLETED')
            ORDER BY COALESCE(completed_at, opened_at) DESC, order_id DESC
            """;

    private final DataSource dataSource;

    public JdbcDashboardRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public DashboardModels.Snapshot loadSnapshot() {
        try (Connection connection = dataSource.getConnection()) {
            return new DashboardModels.Snapshot(loadSummary(connection), loadActivity(connection));
        } catch (SQLException exception) {
            throw new DatabaseException("Unable to load dashboard data.", exception);
        }
    }

    private DashboardModels.Summary loadSummary(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Dashboard summary query returned no row.");
            }
            return new DashboardModels.Summary(result.getBigDecimal("today_sales"),
                    result.getLong("today_orders"), result.getLong("active_tables"),
                    result.getLong("low_stock_items"));
        }
    }

    private List<DashboardModels.Activity> loadActivity(Connection connection) throws SQLException {
        List<DashboardModels.Activity> activity = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(ACTIVITY_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                activity.add(new DashboardModels.Activity(result.getString("order_number"),
                        result.getString("order_type"), result.getString("status"),
                        result.getBigDecimal("total_amount"),
                        result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return activity;
    }
}
