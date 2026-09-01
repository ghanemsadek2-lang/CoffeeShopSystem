package com.coffeeshop.controller;

import com.coffeeshop.model.NavigationItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.Objects;
import java.util.function.Consumer;

/** Handles dashboard quick actions; metric cards deliberately contain no fabricated data. */
public final class DashboardController {
    private final Consumer<NavigationItem> navigationHandler;

    @FXML private Button newOrderButton;
    @FXML private Button viewTablesButton;

    public DashboardController(Consumer<NavigationItem> navigationHandler) {
        this.navigationHandler = Objects.requireNonNull(navigationHandler);
    }

    @FXML private void initialize() {
        newOrderButton.setOnAction(event -> navigationHandler.accept(NavigationItem.POS));
        viewTablesButton.setOnAction(event -> navigationHandler.accept(NavigationItem.TABLES));
    }
}
