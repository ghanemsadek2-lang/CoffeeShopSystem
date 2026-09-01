package com.coffeeshop.controller;

import com.coffeeshop.model.OrderStatus;
import com.coffeeshop.model.OrderSummary;
import com.coffeeshop.service.OrderManagementService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

public final class OrdersController {
    private final OrderManagementService service;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private TableView<OrderSummary> ordersTable;
    @FXML private TableColumn<OrderSummary,String> numberColumn, sourceColumn, typeColumn, statusColumn, openedColumn, totalColumn, tableColumn;
    @FXML private ListView<String> detailList;
    @FXML private Label messageLabel;
    @FXML private Button cancelButton;
    public OrdersController(OrderManagementService service) { this.service = service; }

    @FXML private void initialize() {
        numberColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().number()));
        sourceColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().source()));
        typeColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().type().name()));
        statusColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().status().name()));
        openedColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().openedAt().toString().replace('T',' ')));
        totalColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().total().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        tableColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().tableName() == null ? "-" : v.getValue().tableName()));
        statusFilter.getItems().setAll("OPEN", "COMPLETED", "CANCELLED", "ALL");
        statusFilter.setValue("OPEN");
        statusFilter.valueProperty().addListener((o,a,b) -> refresh());
        ordersTable.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> loadDetails(b));
        refresh();
    }

    @FXML private void refresh() {
        OrderStatus status = "ALL".equals(statusFilter.getValue()) ? null : OrderStatus.valueOf(statusFilter.getValue());
        CompletableFuture.supplyAsync(() -> service.orders(status)).whenComplete((orders,error) -> Platform.runLater(() -> {
            if (error != null) { messageLabel.setText("Unable to load orders."); return; }
            ordersTable.getItems().setAll(orders); detailList.getItems().clear();
        }));
    }

    private void loadDetails(OrderSummary order) {
        cancelButton.setDisable(order == null || order.status() != OrderStatus.OPEN);
        if (order == null) return;
        CompletableFuture.supplyAsync(() -> service.details(order.id())).whenComplete((details,error) -> Platform.runLater(() -> {
            if (error == null && details.isPresent()) {
                detailList.getItems().setAll(details.get().lineDescriptions());
                OrderSummary value = details.get().order();
                messageLabel.setText(value.number() + " - " + value.source() + " - " + value.type() + " - " + value.status()
                        + (value.tableName() == null ? "" : " - " + value.tableName()));
            } else messageLabel.setText("Unable to load order details.");
        }));
    }

    @FXML private void cancel() {
        OrderSummary order = ordersTable.getSelectionModel().getSelectedItem(); if (order == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Cancel " + order.number() + "?", ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText("Cancel open order");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        CompletableFuture.runAsync(() -> service.cancel(order)).whenComplete((ignored,error) -> Platform.runLater(() -> {
            messageLabel.setText(error == null ? "Order cancelled." : "Unable to cancel this order."); refresh();
        }));
    }
}
