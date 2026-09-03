package com.coffeeshop.controller;

import com.coffeeshop.model.DashboardModels;
import com.coffeeshop.model.NavigationItem;
import com.coffeeshop.service.DashboardService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Loads the operational dashboard and handles its quick actions. */
public final class DashboardController {
    private static final DateTimeFormatter ACTIVITY_TIME = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final DashboardService dashboardService;
    private final Consumer<NavigationItem> navigationHandler;

    @FXML private Label todaySalesLabel;
    @FXML private Label todayOrdersLabel;
    @FXML private Label activeTablesLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label dashboardMessageLabel;
    @FXML private VBox recentActivityBox;
    @FXML private Button newOrderButton;
    @FXML private Button viewTablesButton;

    public DashboardController(DashboardService dashboardService, Consumer<NavigationItem> navigationHandler) {
        this.dashboardService = Objects.requireNonNull(dashboardService);
        this.navigationHandler = Objects.requireNonNull(navigationHandler);
    }

    @FXML private void initialize() {
        newOrderButton.setOnAction(event -> navigationHandler.accept(NavigationItem.POS));
        viewTablesButton.setOnAction(event -> navigationHandler.accept(NavigationItem.TABLES));
        refresh();
    }

    private void refresh() {
        dashboardMessageLabel.setText("Loading dashboard…");
        CompletableFuture.supplyAsync(dashboardService::snapshot)
                .whenComplete((snapshot, failure) -> Platform.runLater(() -> {
                    if (failure != null) {
                        dashboardMessageLabel.setText("Unable to load dashboard data.");
                        return;
                    }
                    show(snapshot);
                }));
    }

    private void show(DashboardModels.Snapshot snapshot) {
        DashboardModels.Summary summary = snapshot.summary();
        todaySalesLabel.setText(money(summary.todaySales()));
        todayOrdersLabel.setText(Long.toString(summary.todayOrders()));
        activeTablesLabel.setText(Long.toString(summary.activeTables()));
        lowStockLabel.setText(Long.toString(summary.lowStockItems()));
        recentActivityBox.getChildren().clear();
        if (snapshot.recentActivity().isEmpty()) {
            Label emptyTitle = new Label("No recent activity");
            emptyTitle.getStyleClass().add("empty-title");
            Label emptyCopy = new Label("Completed and active orders will appear here.");
            emptyCopy.getStyleClass().add("empty-copy");
            emptyCopy.setWrapText(true);
            recentActivityBox.getChildren().addAll(emptyTitle, emptyCopy);
        } else {
            snapshot.recentActivity().forEach(activity -> recentActivityBox.getChildren().add(activityRow(activity)));
        }
        dashboardMessageLabel.setText("");
    }

    private static VBox activityRow(DashboardModels.Activity activity) {
        Label title = new Label(activity.orderNumber() + " · " + activity.status().replace('_', ' '));
        title.getStyleClass().add("empty-title");
        Label details = new Label(activity.orderType().replace('_', ' ') + " · "
                + money(activity.total()) + " · " + ACTIVITY_TIME.format(activity.occurredAt()) + " UTC");
        details.getStyleClass().add("empty-copy");
        details.setWrapText(true);
        return new VBox(3, title, details);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
