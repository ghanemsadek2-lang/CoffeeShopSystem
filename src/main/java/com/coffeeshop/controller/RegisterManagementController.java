package com.coffeeshop.controller;

import com.coffeeshop.dto.RegisterCommands;
import com.coffeeshop.exception.DatabaseException;
import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.RegisterModels;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.service.RegisterService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** JavaFX coordination for register configuration and the authenticated cashier's shift. */
public final class RegisterManagementController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterManagementController.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RegisterService service;
    private final ApplicationSession session;
    private RegisterModels.Dashboard dashboard;

    @FXML private Label shiftStatusLabel;
    @FXML private Label shiftRegisterLabel;
    @FXML private Label shiftOpenedLabel;
    @FXML private Label shiftOpeningLabel;
    @FXML private Label shiftExpectedLabel;
    @FXML private Label messageLabel;
    @FXML private Button openShiftButton;
    @FXML private Button cashInButton;
    @FXML private Button cashOutButton;
    @FXML private Button closeShiftButton;
    @FXML private Button addRegisterButton;
    @FXML private Button editRegisterButton;
    @FXML private Button toggleRegisterButton;
    @FXML private TableView<RegisterModels.Shift> shiftsTable;
    @FXML private TableView<RegisterModels.CashMovement> movementsTable;
    @FXML private TableView<RegisterModels.Register> registersTable;
    @FXML private TableColumn<RegisterModels.Shift, String> shiftRegisterColumn;
    @FXML private TableColumn<RegisterModels.Shift, String> shiftUserColumn;
    @FXML private TableColumn<RegisterModels.Shift, String> shiftOpenedColumn;
    @FXML private TableColumn<RegisterModels.Shift, String> shiftStatusColumn;
    @FXML private TableColumn<RegisterModels.Shift, String> shiftDifferenceColumn;
    @FXML private TableColumn<RegisterModels.CashMovement, String> movementTimeColumn;
    @FXML private TableColumn<RegisterModels.CashMovement, String> movementTypeColumn;
    @FXML private TableColumn<RegisterModels.CashMovement, String> movementAmountColumn;
    @FXML private TableColumn<RegisterModels.CashMovement, String> movementReasonColumn;
    @FXML private TableColumn<RegisterModels.Register, String> registerCodeColumn;
    @FXML private TableColumn<RegisterModels.Register, String> registerNameColumn;
    @FXML private TableColumn<RegisterModels.Register, String> registerOrderColumn;
    @FXML private TableColumn<RegisterModels.Register, String> registerStatusColumn;

    public RegisterManagementController(RegisterService service, ApplicationSession session) {
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
    }

    @FXML
    private void initialize() {
        shiftRegisterColumn.setCellValueFactory(value -> text(value.getValue().registerName()));
        shiftUserColumn.setCellValueFactory(value -> text(value.getValue().openedByUsername()));
        shiftOpenedColumn.setCellValueFactory(value -> text(value.getValue().openedAt().format(DATE_TIME)));
        shiftStatusColumn.setCellValueFactory(value -> text(value.getValue().status()));
        shiftDifferenceColumn.setCellValueFactory(value -> text(moneyOrDash(value.getValue().difference())));
        movementTimeColumn.setCellValueFactory(value -> text(value.getValue().movedAt().format(DATE_TIME)));
        movementTypeColumn.setCellValueFactory(value -> text(value.getValue().type().name().replace('_', ' ')));
        movementAmountColumn.setCellValueFactory(value -> text(money(value.getValue().amount())));
        movementReasonColumn.setCellValueFactory(value -> text(value.getValue().reason()));
        registerCodeColumn.setCellValueFactory(value -> text(value.getValue().code()));
        registerNameColumn.setCellValueFactory(value -> text(value.getValue().name()));
        registerOrderColumn.setCellValueFactory(value -> text(Integer.toString(value.getValue().displayOrder())));
        registerStatusColumn.setCellValueFactory(value -> text(value.getValue().active() ? "Active" : "Inactive"));

        shiftsTable.setPlaceholder(new Label("No register shift history is available."));
        movementsTable.setPlaceholder(new Label("No manual cash movements for the current shift."));
        registersTable.setPlaceholder(new Label("No registers have been configured."));
        registersTable.getSelectionModel().selectedItemProperty().addListener((ignored, oldValue, value) -> updateButtons());
        refresh();
    }

    @FXML
    private void refresh() {
        message("Loading register information...", false);
        CompletableFuture.supplyAsync(() -> service.dashboard(user()))
                .whenComplete((value, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        fail("Unable to load register information.", error);
                        return;
                    }
                    dashboard = value;
                    render();
                    message("Register information refreshed.", false);
                }));
    }

    private void render() {
        RegisterModels.Shift current = dashboard.currentShift();
        shiftStatusLabel.setText(current == null ? "No open shift" : "OPEN");
        shiftRegisterLabel.setText(current == null ? "-" : current.registerName() + " (" + current.registerCode() + ")");
        shiftOpenedLabel.setText(current == null ? "-" : current.openedAt().format(DATE_TIME) + " UTC");
        shiftOpeningLabel.setText(current == null ? "0.00" : money(current.openingCash()));
        shiftExpectedLabel.setText(current == null ? "0.00" : money(current.expectedCash()));
        shiftsTable.getItems().setAll(dashboard.shiftHistory());
        movementsTable.getItems().setAll(dashboard.currentMovements());
        registersTable.getItems().setAll(dashboard.registers());
        updateButtons();
    }

    private void updateButtons() {
        boolean loaded = dashboard != null;
        boolean open = loaded && dashboard.currentShift() != null;
        boolean manager = service.canManage(user());
        openShiftButton.setDisable(!loaded || open || dashboard.registers().stream().noneMatch(RegisterModels.Register::active));
        closeShiftButton.setDisable(!open);
        cashInButton.setDisable(!open || !manager);
        cashOutButton.setDisable(!open || !manager);
        addRegisterButton.setDisable(!manager);
        RegisterModels.Register selected = registersTable.getSelectionModel().getSelectedItem();
        editRegisterButton.setDisable(!manager || selected == null);
        toggleRegisterButton.setDisable(!manager || selected == null);
        if (selected != null) {
            toggleRegisterButton.setText(selected.active() ? "Deactivate" : "Activate");
        }
    }

    @FXML
    private void openShift() {
        List<RegisterModels.Register> active = dashboard.registers().stream().filter(RegisterModels.Register::active).toList();
        ComboBox<RegisterModels.Register> register = combo(active, active.isEmpty() ? null : active.getFirst());
        TextField opening = field("0.0000");
        formDialog("Open register shift", List.of(row("Register", register), row("Opening cash", opening)),
                () -> new OpenInput(register.getValue(), decimal(opening, "Opening cash")))
                .ifPresent(input -> write(() -> service.openShift(user(), input.register().id(), input.openingCash()),
                        "Register shift opened."));
    }

    @FXML private void cashIn() { cashMovement(RegisterModels.CashMovementType.CASH_IN); }
    @FXML private void cashOut() { cashMovement(RegisterModels.CashMovementType.CASH_OUT); }

    private void cashMovement(RegisterModels.CashMovementType type) {
        RegisterModels.Shift shift = dashboard.currentShift();
        if (shift == null) return;
        TextField amount = field("");
        TextField reason = field("");
        TextField reference = field("");
        TextArea notes = area("");
        formDialog(type == RegisterModels.CashMovementType.CASH_IN ? "Record cash in" : "Record cash out",
                List.of(row("Amount", amount), row("Reason", reason), row("Reference", reference), row("Notes", notes)),
                () -> new MovementInput(decimal(amount, "Amount"), reason.getText(), reference.getText(), notes.getText()))
                .ifPresent(input -> {
                    String action = type == RegisterModels.CashMovementType.CASH_IN ? "add cash to" : "remove cash from";
                    if (confirm("Confirm that you want to " + action + " the open shift?", "Confirm cash movement")) {
                        write(() -> service.recordMovement(user(), shift.id(), type, input.amount(), input.reason(),
                                input.reference(), input.notes()), "Cash movement recorded.");
                    }
                });
    }

    @FXML
    private void closeShift() {
        RegisterModels.Shift shift = dashboard.currentShift();
        if (shift == null) return;
        TextField actual = field(shift.expectedCash().toPlainString());
        TextField reason = field("");
        Label expected = new Label(money(shift.expectedCash()));
        formDialog("Close register shift", List.of(row("Expected cash", expected), row("Actual cash", actual),
                        row("Variance reason", reason)),
                () -> new CloseInput(decimal(actual, "Actual closing cash"), reason.getText()))
                .ifPresent(input -> {
                    BigDecimal variance = input.actualCash().subtract(shift.expectedCash());
                    String detail = "Expected: " + money(shift.expectedCash()) + "\nActual: " + money(input.actualCash())
                            + "\nVariance: " + money(variance) + "\n\nClose this shift?";
                    if (confirm(detail, "Confirm shift closing")) {
                        writeVoid(() -> service.closeShift(user(), shift, input.actualCash(), input.varianceReason()),
                                "Register shift closed.");
                    }
                });
    }

    @FXML private void addRegister() { registerDialog(null).ifPresent(command -> write(() -> service.saveRegister(user(), command), "Register saved.")); }

    @FXML
    private void editRegister() {
        RegisterModels.Register selected = registersTable.getSelectionModel().getSelectedItem();
        if (selected != null) registerDialog(selected).ifPresent(command -> write(() -> service.saveRegister(user(), command), "Register saved."));
    }

    @FXML
    private void toggleRegister() {
        RegisterModels.Register selected = registersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String action = selected.active() ? "Deactivate" : "Activate";
        if (confirm(action + " " + selected.name() + "?", "Update register status")) {
            writeVoid(() -> service.setRegisterActive(user(), selected, !selected.active()), "Register status updated.");
        }
    }

    private Optional<RegisterCommands.Register> registerDialog(RegisterModels.Register value) {
        TextField code = field(value == null ? "" : value.code());
        TextField name = field(value == null ? "" : value.name());
        TextField order = field(value == null ? "0" : Integer.toString(value.displayOrder()));
        return formDialog(value == null ? "Add register" : "Edit register",
                List.of(row("Register code", code), row("Register name", name), row("Display order", order)),
                () -> new RegisterCommands.Register(value == null ? null : value.id(), code.getText(), name.getText(),
                        integer(order, "Display order"), value == null || value.active(), value == null ? null : value.rowVersion()));
    }

    private <T> Optional<T> formDialog(String title, List<Node[]> rows, Supplier<T> result) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(9);
        ColumnConstraints labels = new ColumnConstraints(130, 130, 130);
        labels.setHalignment(HPos.RIGHT);
        ColumnConstraints controls = new ColumnConstraints(230, 320, Double.MAX_VALUE);
        controls.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, controls);
        for (int index = 0; index < rows.size(); index++) {
            grid.add(rows.get(index)[0], 0, index);
            grid.add(rows.get(index)[1], 1, index);
            GridPane.setHgrow(rows.get(index)[1], Priority.ALWAYS);
        }
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("status-error");
        dialog.getDialogPane().setContent(new VBox(8, grid, error));
        dialog.getDialogPane().setPrefWidth(510);
        AtomicReference<T> validated = new AtomicReference<>();
        Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validated.set(result.get());
                error.setText("");
            } catch (RuntimeException exception) {
                error.setText(exception.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK ? validated.get() : null);
        return dialog.showAndWait();
    }

    private void write(java.util.concurrent.Callable<Long> operation, String success) {
        message("Saving...", false);
        CompletableFuture.supplyAsync(() -> {
            try { return operation.call(); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }).whenComplete((ignored, error) -> Platform.runLater(() -> completed(success, error)));
    }

    private void writeVoid(Runnable operation, String success) {
        write(() -> { operation.run(); return 0L; }, success);
    }

    private void completed(String success, Throwable error) {
        if (error != null) {
            fail("Unable to save this register change.", error);
            return;
        }
        message(success, false);
        refresh();
    }

    private void fail(String fallback, Throwable error) {
        Throwable cause = unwrap(error);
        LOGGER.error("Register operation failed ({}).", cause.getClass().getSimpleName());
        boolean safe = cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                || cause instanceof SecurityException || cause instanceof DatabaseException;
        message(safe ? cause.getMessage() : fallback, true);
    }

    private AuthenticatedUser user() {
        return session.currentUser().orElseThrow(() -> new IllegalStateException("An authenticated session is required."));
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && (cause instanceof CompletionException || cause instanceof ExecutionException)) {
            cause = cause.getCause();
        }
        return cause;
    }

    private boolean confirm(String content, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText(header);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void message(String value, boolean error) {
        messageLabel.setText(value);
        messageLabel.getStyleClass().setAll(error ? "status-error" : "status-message");
    }

    private static TextField field(String value) { TextField field = new TextField(value); field.setMaxWidth(Double.MAX_VALUE); return field; }
    private static TextArea area(String value) { TextArea area = new TextArea(value); area.setPrefRowCount(2); area.setWrapText(true); return area; }
    private static Node[] row(String label, Node control) { Label fieldLabel = new Label(label); fieldLabel.setMinWidth(Label.USE_PREF_SIZE); return new Node[]{fieldLabel, control}; }
    private static <T> ComboBox<T> combo(Collection<T> values, T selected) { ComboBox<T> box = new ComboBox<>(); box.getItems().setAll(values); box.setValue(selected); box.setMaxWidth(Double.MAX_VALUE); return box; }
    private static SimpleStringProperty text(String value) { return new SimpleStringProperty(value); }
    private static String money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String moneyOrDash(BigDecimal value) { return value == null ? "-" : money(value); }
    private static BigDecimal decimal(TextField field, String label) { try { return new BigDecimal(field.getText().trim()); } catch (Exception exception) { throw new IllegalArgumentException(label + " must be a valid number."); } }
    private static int integer(TextField field, String label) { try { return Integer.parseInt(field.getText().trim()); } catch (Exception exception) { throw new IllegalArgumentException(label + " must be a whole number."); } }

    private record OpenInput(RegisterModels.Register register, BigDecimal openingCash) {
        private OpenInput { if (register == null) throw new IllegalArgumentException("Select an active register."); }
    }
    private record MovementInput(BigDecimal amount, String reason, String reference, String notes) { }
    private record CloseInput(BigDecimal actualCash, String varianceReason) { }
}
