package com.coffeeshop.controller;

import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.service.AuthenticationService;
import com.coffeeshop.service.LoginResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates the login view without owning authentication or persistence logic. */
public final class LoginController implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final String UNAVAILABLE_MESSAGE =
            "Unable to sign in right now. Please try again or contact a manager.";

    private final AuthenticationService authenticationService;
    private final ApplicationSession session;
    private final ExecutorService authenticationExecutor;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private StackPane passwordContainer;
    @FXML
    private CheckBox showPasswordCheckBox;
    @FXML
    private Button signInButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private boolean busy;
    private boolean authenticated;

    public LoginController(AuthenticationService authenticationService, ApplicationSession session) {
        this(authenticationService, session, Executors.newSingleThreadExecutor(
                Thread.ofPlatform().name("authentication-worker").daemon(true).factory()
        ));
    }

    LoginController(AuthenticationService authenticationService, ApplicationSession session,
                    ExecutorService authenticationExecutor) {
        this.authenticationService = Objects.requireNonNull(authenticationService);
        this.session = Objects.requireNonNull(session);
        this.authenticationExecutor = Objects.requireNonNull(authenticationExecutor);
    }

    @FXML
    private void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        showPasswordCheckBox.selectedProperty().addListener((observable, oldValue, show) -> {
            visiblePasswordField.setVisible(show);
            visiblePasswordField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            if (show) {
                visiblePasswordField.requestFocus();
                visiblePasswordField.positionCaret(visiblePasswordField.getLength());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getLength());
            }
        });
        setBusy(false);
        Platform.runLater(usernameField::requestFocus);
    }

    @FXML
    private void signIn() {
        if (busy || authenticated) {
            return;
        }

        clearStatus();
        String username = usernameField.getText();
        String passwordText = passwordField.getText();
        char[] password = passwordText == null ? null : passwordText.toCharArray();
        passwordText = null;
        clearPasswordFields();

        setBusy(true);
        Task<LoginResult> authenticationTask = new Task<>() {
            @Override
            protected LoginResult call() {
                return authenticationService.login(username, password);
            }
        };
        authenticationTask.setOnSucceeded(event -> handleResult(authenticationTask.getValue()));
        authenticationTask.setOnFailed(event -> handleFailure(authenticationTask.getException()));
        authenticationTask.setOnCancelled(event -> setBusy(false));

        try {
            authenticationExecutor.execute(authenticationTask);
        } catch (RuntimeException exception) {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            handleFailure(exception);
        }
    }

    private void handleResult(LoginResult result) {
        setBusy(false);
        if (!result.successful()) {
            showStatus(result.message(), true);
            passwordField.requestFocus();
            return;
        }

        try {
            AuthenticatedUser user = result.user().orElseThrow();
            session.establish(user);
            authenticated = true;
            updateControlState();
            showStatus("Signed in successfully. Welcome, " + user.displayName() + '.', false);
        } catch (RuntimeException exception) {
            LOGGER.error("Authenticated session could not be established ({}).",
                    exception.getClass().getSimpleName());
            showStatus(UNAVAILABLE_MESSAGE, true);
        }
    }

    private void handleFailure(Throwable failure) {
        setBusy(false);
        String failureType = failure == null ? "UnknownFailure" : failure.getClass().getSimpleName();
        LOGGER.error("Authentication request failed ({}).", failureType);
        showStatus(UNAVAILABLE_MESSAGE, true);
        passwordField.requestFocus();
    }

    private void setBusy(boolean value) {
        busy = value;
        progressIndicator.setVisible(value);
        progressIndicator.setManaged(value);
        updateControlState();
        if (value) {
            showStatus("Signing in securely…", false);
        }
    }

    private void updateControlState() {
        boolean disabled = busy || authenticated;
        usernameField.setDisable(disabled);
        passwordContainer.setDisable(disabled);
        showPasswordCheckBox.setDisable(disabled);
        signInButton.setDisable(disabled);
    }

    private void clearPasswordFields() {
        passwordField.clear();
        visiblePasswordField.clear();
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.pseudoClassStateChanged(ERROR, false);
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.pseudoClassStateChanged(ERROR, error);
    }

    @Override
    public void close() {
        authenticationExecutor.shutdownNow();
        if (passwordField != null) {
            clearPasswordFields();
        }
    }
}
