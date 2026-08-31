package com.coffeeshop.app;

import com.coffeeshop.controller.LoginController;
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
    private static final String LOGIN_VIEW = "/fxml/login-view.fxml";
    private static final String LOGIN_STYLESHEET = "/css/login.css";

    private ApplicationContext applicationContext;
    private StartupException startupFailure;
    private LoginController loginController;

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
                CoffeeShopApplication.class.getResource(LOGIN_VIEW),
                "Missing required FXML resource: " + LOGIN_VIEW
        );
        URL stylesheetUrl = Objects.requireNonNull(
                CoffeeShopApplication.class.getResource(LOGIN_STYLESHEET),
                "Missing required stylesheet resource: " + LOGIN_STYLESHEET
        );

        FXMLLoader loader = new FXMLLoader(viewUrl);
        loader.setControllerFactory(type -> createController(type));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1120, 700);
        scene.getStylesheets().add(stylesheetUrl.toExternalForm());

        primaryStage.setTitle("Coffee Shop Management System — Sign In");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (loginController != null) {
            loginController.close();
            loginController = null;
        }
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
        }
    }

    private Object createController(Class<?> type) {
        if (type == LoginController.class) {
            loginController = new LoginController(
                    applicationContext.authenticationService(),
                    applicationContext.session()
            );
            return loginController;
        }
        throw new IllegalArgumentException("Unsupported FXML controller: " + type.getName());
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
