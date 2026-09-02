package com.coffeeshop.controller;

import com.coffeeshop.model.LoyaltyModels.*;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.service.LoyaltyService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class LoyaltyController {
    private final LoyaltyService service;
    private final ApplicationSession session;
    @FXML private TextField searchField;
    @FXML private TableView<Account> accountTable;
    @FXML private TableColumn<Account,String> membershipColumn, customerColumn, balanceColumn, lifetimeColumn, statusColumn;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction,String> timeColumn, typeColumn, pointsColumn, userColumn, reasonColumn;
    @FXML private Label messageLabel;
    @FXML private Button earnButton, redeemButton;
    private Catalog catalog = new Catalog(java.util.List.of(), java.util.List.of(), java.util.List.of());

    public LoyaltyController(LoyaltyService service, ApplicationSession session) {
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
    }

    @FXML private void initialize() {
        membershipColumn.setCellValueFactory(v -> text(v.getValue().membershipNumber()));
        customerColumn.setCellValueFactory(v -> text(v.getValue().customerName()));
        balanceColumn.setCellValueFactory(v -> text(Long.toString(v.getValue().pointsBalance())));
        lifetimeColumn.setCellValueFactory(v -> text(Long.toString(v.getValue().lifetimePointsEarned())));
        statusColumn.setCellValueFactory(v -> text(v.getValue().status()));
        timeColumn.setCellValueFactory(v -> text(v.getValue().occurredAt().toString().replace('T', ' ')));
        typeColumn.setCellValueFactory(v -> text(v.getValue().type()));
        pointsColumn.setCellValueFactory(v -> text((v.getValue().pointsDelta() > 0 ? "+" : "") + v.getValue().pointsDelta()));
        userColumn.setCellValueFactory(v -> text(v.getValue().recordedBy() == null ? "System" : v.getValue().recordedBy()));
        reasonColumn.setCellValueFactory(v -> text(v.getValue().reason() == null ? "-" : v.getValue().reason()));
        searchField.textProperty().addListener((o, a, b) -> display());
        accountTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> selection(b));
        refresh();
    }

    @FXML private void refresh() {
        CompletableFuture.supplyAsync(service::catalog).whenComplete((value, error) -> Platform.runLater(() -> {
            if (error != null) { messageLabel.setText("Unable to load loyalty accounts."); return; }
            catalog = value;
            display();
            transactionTable.getItems().setAll(value.transactions());
            messageLabel.setText(value.accounts().isEmpty() ? "No loyalty accounts are enrolled." : "");
        }));
    }

    private void display() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        accountTable.getItems().setAll(catalog.accounts().stream().filter(a -> query.isEmpty()
                || a.membershipNumber().toLowerCase().contains(query)
                || a.customerName().toLowerCase().contains(query)
                || a.customerNumber().toLowerCase().contains(query)).toList());
        selection(accountTable.getSelectionModel().getSelectedItem());
    }

    @FXML private void enroll() {
        if (catalog.availableCustomers().isEmpty()) { messageLabel.setText("All active customers already have loyalty accounts."); return; }
        Dialog<Enrollment> dialog = new Dialog<>();
        dialog.setTitle("Enroll customer");
        ButtonType save = new ButtonType("Enroll", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        ComboBox<CustomerOption> customer = new ComboBox<>(); customer.getItems().setAll(catalog.availableCustomers()); customer.setMaxWidth(Double.MAX_VALUE);
        TextField membership = new TextField(); membership.setPromptText("Stable membership number");
        GridPane pane = form(); field(pane, 0, "Customer", customer); field(pane, 1, "Membership number", membership);
        dialog.getDialogPane().setContent(pane); dialog.getDialogPane().setPrefWidth(480);
        dialog.setResultConverter(button -> button == save ? new Enrollment(customer.getValue(), membership.getText()) : null);
        dialog.showAndWait().ifPresent(value -> write(() -> service.enroll(value.customer(), value.membership()), "Customer enrolled."));
    }

    @FXML private void earn() { points("Earn points", true); }
    @FXML private void redeem() { points("Redeem points", false); }

    private void points(String title, boolean earning) {
        Account account = accountTable.getSelectionModel().getSelectedItem();
        if (account == null) { messageLabel.setText("Select a loyalty account."); return; }
        Dialog<PointsInput> dialog = new Dialog<>(); dialog.setTitle(title);
        ButtonType save = new ButtonType(earning ? "Earn" : "Redeem", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        TextField points = new TextField(), reason = new TextField(); TextArea notes = new TextArea(); notes.setPrefRowCount(3);
        GridPane pane = form(); field(pane, 0, "Account", new Label(account.toString())); field(pane, 1, "Points", points);
        field(pane, 2, "Reason", reason); field(pane, 3, "Notes", notes);
        dialog.getDialogPane().setContent(pane); dialog.getDialogPane().setPrefWidth(500);
        dialog.setResultConverter(button -> button == save ? new PointsInput(parse(points.getText()), reason.getText(), notes.getText()) : null);
        dialog.showAndWait().ifPresent(value -> write(() -> {
            if (earning) service.earn(account, value.points(), value.reason(), value.notes(), userId());
            else service.redeem(account, value.points(), value.reason(), value.notes(), userId());
        }, earning ? "Points earned." : "Points redeemed."));
    }

    private void write(Runnable operation, String success) {
        CompletableFuture.runAsync(operation).whenComplete((value, error) -> Platform.runLater(() -> {
            if (error == null) { refresh(); messageLabel.setText(success); }
            else messageLabel.setText(message(error));
        }));
    }
    private void selection(Account account) {
        boolean disabled = account == null || !"ACTIVE".equals(account.status());
        earnButton.setDisable(disabled); redeemButton.setDisable(disabled);
        if (account != null) transactionTable.getItems().setAll(catalog.transactions().stream()
                .filter(t -> t.accountId() == account.id()).toList());
        else transactionTable.getItems().setAll(catalog.transactions());
    }
    private long userId() { return session.currentUser().orElseThrow().userId(); }
    private static long parse(String value) { try { return Long.parseLong(value == null ? "" : value.trim()); } catch (Exception e) { return 0; } }
    private static GridPane form() { GridPane pane = new GridPane(); pane.setHgap(12); pane.setVgap(8); pane.setPadding(new Insets(8)); return pane; }
    private static void field(GridPane pane, int row, String label, Control control) { pane.add(new Label(label), 0, row); control.setMaxWidth(Double.MAX_VALUE); pane.add(control, 1, row); }
    private static SimpleStringProperty text(String value) { return new SimpleStringProperty(value); }
    private static String message(Throwable error) { Throwable cause = error; while (cause.getCause() != null) cause = cause.getCause(); return cause instanceof IllegalArgumentException || cause instanceof IllegalStateException ? cause.getMessage() : "Unable to update loyalty points."; }
    private record Enrollment(CustomerOption customer, String membership) {}
    private record PointsInput(long points, String reason, String notes) {}
}
