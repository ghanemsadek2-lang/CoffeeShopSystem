package com.coffeeshop.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/** Application bootstrap for the Coffee Shop Management System. */
public final class CoffeeShopApplication extends Application {

    private static final String MAIN_VIEW = "/fxml/main-view.fxml";
    private static final String STYLESHEET = "/css/application.css";

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL viewUrl = Objects.requireNonNull(
                CoffeeShopApplication.class.getResource(MAIN_VIEW),
                "Missing required FXML resource: " + MAIN_VIEW
        );
        URL stylesheetUrl = Objects.requireNonNull(
                CoffeeShopApplication.class.getResource(STYLESHEET),
                "Missing required stylesheet resource: " + STYLESHEET
        );

        Parent root = FXMLLoader.load(viewUrl);
        Scene scene = new Scene(root, 960, 600);
        scene.getStylesheets().add(stylesheetUrl.toExternalForm());

        primaryStage.setTitle("Coffee Shop Management System");
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(480);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
