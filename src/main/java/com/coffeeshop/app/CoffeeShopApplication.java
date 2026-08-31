package com.coffeeshop.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/** Application bootstrap for the Coffee Shop Management System. */
public final class CoffeeShopApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoffeeShopApplication.class);
    private static final String MAIN_VIEW = "/fxml/main-view.fxml";
    private static final String STYLESHEET = "/css/application.css";

    private ApplicationContext applicationContext;
    private StartupException startupFailure;

    @Override
    public void init() {
        try {
            applicationContext = new ApplicationStartup().initialize();
        } catch (StartupException exception) {
            startupFailure = exception;
            LOGGER.error("Application startup failed during infrastructure initialization ({}).",
                    exception.failureType());
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        if (startupFailure != null) {
            showStartupFailure(startupFailure);
            return;
        }

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

    @Override
    public void stop() {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
        }
    }

    private static void showStartupFailure(StartupException failure) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Coffee Shop Management System");
        alert.setHeaderText("The application could not start");
        alert.setContentText(failure.userMessage());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
