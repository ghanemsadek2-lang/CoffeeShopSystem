package com.coffeeshop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Objects;

/** Presents a neutral module placeholder without simulated business data. */
public final class ModulePlaceholderController {
    private final String moduleName;
    @FXML private Label moduleNameLabel;

    public ModulePlaceholderController(String moduleName) {
        this.moduleName = Objects.requireNonNull(moduleName);
    }

    @FXML private void initialize() {
        moduleNameLabel.setText(moduleName);
    }
}
