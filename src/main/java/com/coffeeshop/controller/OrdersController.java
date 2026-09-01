package com.coffeeshop.controller;

import com.coffeeshop.dto.CheckoutCommands;
import com.coffeeshop.model.CheckoutModels;
import com.coffeeshop.model.OrderStatus;
import com.coffeeshop.model.OrderSummary;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.service.CheckoutService;
import com.coffeeshop.service.OrderManagementService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class OrdersController {
    private final OrderManagementService service;
    private final CheckoutService checkoutService;
    private final ApplicationSession session;
    @FXML private ChoiceBox<String> statusFilter;
    @FXML private TableView<OrderSummary> ordersTable;
    @FXML private TableColumn<OrderSummary,String> numberColumn, sourceColumn, typeColumn, statusColumn, openedColumn, totalColumn, tableColumn;
    @FXML private ListView<String> detailList;
    @FXML private Label messageLabel;
    @FXML private Button cancelButton;
    @FXML private Button checkoutButton;
    public OrdersController(OrderManagementService service, CheckoutService checkoutService,
                            ApplicationSession session) {
        this.service = service;
        this.checkoutService = checkoutService;
        this.session = session;
    }

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
        checkoutButton.setDisable(order == null || order.status() != OrderStatus.OPEN);
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

    @FXML private void checkout() {
        OrderSummary order = ordersTable.getSelectionModel().getSelectedItem();
        if (order == null || order.status() != OrderStatus.OPEN) return;
        var user = session.currentUser().orElseThrow();
        setActionsDisabled(true);
        CompletableFuture.supplyAsync(() -> checkoutService.prepare(order.id(), user))
                .whenComplete((context, error) -> Platform.runLater(() -> {
                    setActionsDisabled(false);
                    if (error != null) {
                        messageLabel.setText(userMessage(error, "Unable to prepare checkout."));
                        return;
                    }
                    checkoutDialog(context, user.userId()).ifPresent(command -> completeCheckout(command, order));
                }));
    }

    private Optional<CheckoutCommands.Checkout> checkoutDialog(CheckoutModels.Context context, long userId) {
        Dialog<CheckoutCommands.Checkout> dialog = new Dialog<>();
        dialog.setTitle("Complete order");
        dialog.setHeaderText(context.orderNumber() + "   Total " + money(context.total()));
        ButtonType complete = new ButtonType("Complete sale", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, complete);

        Label shift = new Label(context.hasOpenShift()
                ? "Cash register: " + context.currentRegisterName()
                : "No open shift. Cash payment is unavailable until a shift is opened.");
        shift.setWrapText(true);
        shift.getStyleClass().add(context.hasOpenShift() ? "status-message" : "status-error");
        VBox rows = new VBox(8);
        List<PaymentRow> paymentRows = new ArrayList<>();
        Runnable addRow = () -> addPaymentRow(rows, paymentRows, context, paymentRows.isEmpty());
        addRow.run();
        Button add = new Button("Add split payment");
        add.getStyleClass().add("secondary-action");
        add.setOnAction(event -> addRow.run());
        Label validation = new Label();
        validation.setWrapText(true);
        validation.getStyleClass().add("status-error");
        ScrollPane paymentScroll = new ScrollPane(rows);
        paymentScroll.setFitToWidth(true);
        paymentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paymentScroll.setMinHeight(72);
        paymentScroll.setPrefHeight(160);
        paymentScroll.setMaxHeight(Double.MAX_VALUE);
        VBox footer = new VBox(8, add, validation);
        BorderPane content = new BorderPane(paymentScroll, shift, null, footer, null);
        content.setPadding(new Insets(4));
        content.setMinWidth(0);
        content.setPrefHeight(280);
        content.setMaxHeight(360);
        BorderPane.setMargin(shift, new Insets(0, 0, 10, 0));
        BorderPane.setMargin(footer, new Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(620);

        AtomicReference<CheckoutCommands.Checkout> result = new AtomicReference<>();
        Node completeButton = dialog.getDialogPane().lookupButton(complete);
        completeButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                List<CheckoutCommands.Payment> payments = paymentRows.stream()
                        .map(PaymentRow::command).toList();
                result.set(checkoutService.validate(new CheckoutCommands.Checkout(
                        context.orderId(), userId, context.total(), context.orderRowVersion(), payments), context));
                validation.setText("");
            } catch (IllegalArgumentException exception) {
                validation.setText(exception.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == complete ? result.get() : null);
        return dialog.showAndWait();
    }

    private static void addPaymentRow(VBox container, List<PaymentRow> rows,
                                      CheckoutModels.Context context, boolean first) {
        ComboBox<CheckoutModels.PaymentMethod> method = new ComboBox<>();
        method.getItems().setAll(context.paymentMethods());
        method.setMaxWidth(Double.MAX_VALUE);
        CheckoutModels.PaymentMethod initial = context.paymentMethods().stream()
                .filter(value -> !"CASH".equals(value.code()) || context.hasOpenShift())
                .findFirst().orElse(context.paymentMethods().getFirst());
        method.setValue(initial);
        TextField amount = new TextField(first ? context.total().setScale(2, RoundingMode.HALF_UP).toPlainString() : "");
        amount.setPromptText("Amount");
        amount.setPrefColumnCount(9);
        TextField reference = new TextField();
        reference.setPromptText("Reference (optional)");
        Button remove = new Button("Remove");
        HBox line = new HBox(8, method, amount, reference, remove);
        line.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(method, Priority.ALWAYS);
        HBox.setHgrow(reference, Priority.ALWAYS);
        PaymentRow row = new PaymentRow(method, amount, reference);
        rows.add(row);
        container.getChildren().add(line);
        remove.setDisable(first);
        remove.setOnAction(event -> { rows.remove(row); container.getChildren().remove(line); });
    }

    private void completeCheckout(CheckoutCommands.Checkout command, OrderSummary order) {
        setActionsDisabled(true);
        CompletableFuture.runAsync(() -> checkoutService.complete(command))
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    setActionsDisabled(false);
                    if (error == null) {
                        messageLabel.setText("Order " + order.number() + " completed successfully.");
                        refresh();
                    } else messageLabel.setText(userMessage(error, "Unable to complete this order."));
                }));
    }

    private void setActionsDisabled(boolean disabled) {
        OrderSummary selected = ordersTable.getSelectionModel().getSelectedItem();
        boolean unavailable = selected == null || selected.status() != OrderStatus.OPEN;
        checkoutButton.setDisable(disabled || unavailable);
        cancelButton.setDisable(disabled || unavailable);
    }

    private static String userMessage(Throwable error, String fallback) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                || cause instanceof SecurityException ? cause.getMessage() : fallback;
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record PaymentRow(ComboBox<CheckoutModels.PaymentMethod> method,
                              TextField amount, TextField reference) {
        private CheckoutCommands.Payment command() {
            if (method.getValue() == null) throw new IllegalArgumentException("Select a payment method.");
            BigDecimal value;
            try { value = new BigDecimal(amount.getText().trim()); }
            catch (RuntimeException exception) { throw new IllegalArgumentException("Enter a valid payment amount."); }
            return new CheckoutCommands.Payment(method.getValue().id(), value, reference.getText());
        }
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
