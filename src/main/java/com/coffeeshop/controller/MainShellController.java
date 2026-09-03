package com.coffeeshop.controller;

import com.coffeeshop.model.AuthenticatedUser;
import com.coffeeshop.model.NavigationItem;
import com.coffeeshop.security.ApplicationSession;
import com.coffeeshop.security.NavigationPolicy;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Coordinates authenticated shell navigation and session-aware logout. */
public final class MainShellController {

    private final ApplicationSession session;
    private final NavigationPolicy navigationPolicy;
    private final Function<NavigationItem, Node> viewProvider;
    private final Runnable logoutHandler;
    private final Map<NavigationItem, Button> navigationButtons = new EnumMap<>(NavigationItem.class);

    @FXML private VBox navigationBox;
    @FXML private StackPane contentHost;
    @FXML private Label pageTitleLabel;
    @FXML private Label workspaceLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    public MainShellController(ApplicationSession session, NavigationPolicy navigationPolicy,
                               Function<NavigationItem, Node> viewProvider, Runnable logoutHandler) {
        this.session = Objects.requireNonNull(session);
        this.navigationPolicy = Objects.requireNonNull(navigationPolicy);
        this.viewProvider = Objects.requireNonNull(viewProvider);
        this.logoutHandler = Objects.requireNonNull(logoutHandler);
    }

    @FXML
    private void initialize() {
        AuthenticatedUser user = session.currentUser().orElseThrow(
                () -> new IllegalStateException("An authenticated session is required."));
        userNameLabel.setText(user.displayName());
        userRoleLabel.setText(user.roles().stream().map(role -> role.name()).sorted()
                .findFirst().orElse("Team Member"));
        if (navigationPolicy.isCashierWorkspace(user)) {
            workspaceLabel.setText("Cashier Workspace");
            workspaceLabel.getStyleClass().add("cashier-workspace");
        } else if (navigationPolicy.isManagerWorkspace(user)) {
            workspaceLabel.setText("Manager Workspace");
            workspaceLabel.getStyleClass().add("manager-workspace");
        }
        navigationPolicy.allowedItems(user).stream()
                .sorted(java.util.Comparator.comparingInt(Enum::ordinal))
                .forEach(this::addNavigationButton);
        navigateTo(navigationPolicy.defaultItem(user));
    }

    private void addNavigationButton(NavigationItem item) {
        Button button = new Button(item.label());
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.setOnAction(event -> navigateTo(item));
        navigationButtons.put(item, button);
        navigationBox.getChildren().add(button);
    }

    public void navigateTo(NavigationItem item) {
        AuthenticatedUser user = session.currentUser().orElseThrow();
        if (!navigationPolicy.canAccess(user, item)) {
            return;
        }
        contentHost.getChildren().setAll(viewProvider.apply(item));
        pageTitleLabel.setText(item.label());
        navigationButtons.forEach((key, button) ->
                button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), key == item));
    }

    @FXML
    private void logout() {
        session.clear();
        contentHost.getChildren().clear();
        navigationButtons.clear();
        logoutHandler.run();
    }
}
