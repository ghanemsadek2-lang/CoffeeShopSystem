package com.coffeeshop.app;

import com.coffeeshop.controller.LoginController;
import com.coffeeshop.controller.DashboardController;
import com.coffeeshop.controller.MainShellController;
import com.coffeeshop.controller.ModulePlaceholderController;
import com.coffeeshop.controller.PosController;
import com.coffeeshop.controller.OrdersController;
import com.coffeeshop.controller.TablesController;
import com.coffeeshop.controller.MenuManagementController;
import com.coffeeshop.controller.InventoryManagementController;
import com.coffeeshop.controller.RegisterManagementController;
import com.coffeeshop.controller.ReservationsController;
import com.coffeeshop.controller.CustomerManagementController;
import com.coffeeshop.controller.EmployeeManagementController;
import com.coffeeshop.controller.PurchasingController;
import com.coffeeshop.controller.ExpenseController;
import com.coffeeshop.controller.ReportsController;
import com.coffeeshop.controller.SettingsController;
import com.coffeeshop.controller.DiscountController;
import com.coffeeshop.controller.DocumentsController;
import com.coffeeshop.controller.NotificationsController;
import com.coffeeshop.controller.AuditController;
import com.coffeeshop.controller.RefundController;
import com.coffeeshop.controller.LoyaltyController;
import com.coffeeshop.model.NavigationItem;
import com.coffeeshop.security.NavigationPolicy;
import com.coffeeshop.repository.JdbcPosRepository;
import com.coffeeshop.repository.JdbcOrderManagementRepository;
import com.coffeeshop.repository.JdbcOrderDiscountRepository;
import com.coffeeshop.repository.JdbcMenuManagementRepository;
import com.coffeeshop.repository.JdbcInventoryRepository;
import com.coffeeshop.repository.JdbcRegisterRepository;
import com.coffeeshop.repository.JdbcCheckoutRepository;
import com.coffeeshop.repository.JdbcTableManagementRepository;
import com.coffeeshop.repository.JdbcCustomerRepository;
import com.coffeeshop.repository.JdbcEmployeeRepository;
import com.coffeeshop.repository.JdbcPurchasingRepository;
import com.coffeeshop.repository.JdbcExpenseRepository;
import com.coffeeshop.repository.JdbcReportRepository;
import com.coffeeshop.repository.JdbcSettingRepository;
import com.coffeeshop.repository.JdbcDiscountRepository;
import com.coffeeshop.repository.JdbcDocumentRepository;
import com.coffeeshop.repository.JdbcNotificationRepository;
import com.coffeeshop.repository.JdbcAuditRepository;
import com.coffeeshop.repository.JdbcRefundRepository;
import com.coffeeshop.repository.JdbcLoyaltyRepository;
import com.coffeeshop.repository.JdbcDashboardRepository;
import com.coffeeshop.service.PosService;
import com.coffeeshop.service.OrderManagementService;
import com.coffeeshop.service.OrderDiscountService;
import com.coffeeshop.service.MenuManagementService;
import com.coffeeshop.service.InventoryService;
import com.coffeeshop.service.RegisterService;
import com.coffeeshop.service.CheckoutService;
import com.coffeeshop.service.TableManagementService;
import com.coffeeshop.service.CustomerService;
import com.coffeeshop.service.EmployeeService;
import com.coffeeshop.service.PurchasingService;
import com.coffeeshop.service.ExpenseService;
import com.coffeeshop.service.ReportService;
import com.coffeeshop.service.SettingService;
import com.coffeeshop.service.DiscountService;
import com.coffeeshop.service.DocumentService;
import com.coffeeshop.service.NotificationService;
import com.coffeeshop.service.AuditService;
import com.coffeeshop.service.RefundService;
import com.coffeeshop.service.LoyaltyService;
import com.coffeeshop.service.DashboardService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/** Application bootstrap for the Coffee Shop Management System. */
public final class CoffeeShopApplication extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoffeeShopApplication.class);
    private static final String LOGIN_VIEW = "/fxml/login-view.fxml";
    private static final String LOGIN_STYLESHEET = "/css/login.css";
    private static final String SHELL_VIEW = "/fxml/main-shell.fxml";
    private static final String SHELL_STYLESHEET = "/css/shell.css";
    private static final double SCREEN_EDGE_MARGIN = 12.0;

    private ApplicationContext applicationContext;
    private StartupException startupFailure;
    private LoginController loginController;
    private MainShellController mainShellController;
    private Stage primaryStage;

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

        this.primaryStage = primaryStage;
        showLogin();
        primaryStage.show();
    }

    private void showLogin() throws IOException {
        closeLoginController();
        URL viewUrl = Objects.requireNonNull(
                CoffeeShopApplication.class.getResource(LOGIN_VIEW),
                "Missing required FXML resource: " + LOGIN_VIEW
        );
        URL stylesheetUrl = Objects.requireNonNull(
                CoffeeShopApplication.class.getResource(LOGIN_STYLESHEET),
                "Missing required stylesheet resource: " + LOGIN_STYLESHEET
        );

        FXMLLoader loader = new FXMLLoader(viewUrl);
        loader.setControllerFactory(type -> {
            if (type == LoginController.class) {
                loginController = new LoginController(applicationContext.authenticationService(),
                        applicationContext.session(), user -> showMainShellSafely());
                return loginController;
            }
            throw unsupportedController(type);
        });
        Parent root = loader.load();
        Scene scene = new Scene(root, 1040, 680);
        scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        primaryStage.setTitle("Coffee Shop Management System — Sign In");
        primaryStage.setScene(scene);
        fitStageToVisualBounds(1040, 680, 760, 480);
    }

    private void showMainShellSafely() {
        try {
            showMainShell();
        } catch (IOException exception) {
            LOGGER.error("Authenticated shell could not be loaded ({}).", exception.getClass().getSimpleName());
            showNavigationFailure();
        }
    }

    private void showMainShell() throws IOException {
        closeLoginController();
        FXMLLoader loader = new FXMLLoader(resource(SHELL_VIEW));
        loader.setControllerFactory(type -> {
            if (type == MainShellController.class) {
                mainShellController = new MainShellController(applicationContext.session(),
                        new NavigationPolicy(), this::loadModuleView, this::logoutToLogin);
                return mainShellController;
            }
            throw unsupportedController(type);
        });
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 760);
        scene.getStylesheets().add(resource(SHELL_STYLESHEET).toExternalForm());
        primaryStage.setTitle("Coffee Shop POS & Management");
        primaryStage.setScene(scene);
        fitStageToVisualBounds(1200, 760, 800, 500);
    }

    /** Keeps normal (non-maximized) window bounds inside the taskbar-aware screen area. */
    private void fitStageToVisualBounds(double preferredWidth, double preferredHeight,
                                        double requestedMinWidth, double requestedMinHeight) {
        Rectangle2D visualBounds = activeVisualBounds();
        double availableWidth = Math.max(1.0, visualBounds.getWidth() - SCREEN_EDGE_MARGIN * 2);
        double availableHeight = Math.max(1.0, visualBounds.getHeight() - SCREEN_EDGE_MARGIN * 2);

        primaryStage.setMinWidth(Math.min(requestedMinWidth, availableWidth));
        primaryStage.setMinHeight(Math.min(requestedMinHeight, availableHeight));
        if (primaryStage.isMaximized()) {
            return;
        }

        double width = Math.min(preferredWidth, availableWidth);
        double height = Math.min(preferredHeight, availableHeight);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - width) / 2.0);
        primaryStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - height) / 2.0);
    }

    private Rectangle2D activeVisualBounds() {
        if (primaryStage.isShowing()) {
            List<Screen> screens = Screen.getScreensForRectangle(
                    primaryStage.getX(), primaryStage.getY(),
                    Math.max(1.0, primaryStage.getWidth()), Math.max(1.0, primaryStage.getHeight()));
            if (!screens.isEmpty()) {
                return screens.getFirst().getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    private Parent loadModuleView(NavigationItem item) {
        String fxml = item == NavigationItem.DASHBOARD ? "/fxml/dashboard-view.fxml"
                : item == NavigationItem.POS ? "/fxml/pos-view.fxml"
                : item == NavigationItem.ORDERS ? "/fxml/orders-view.fxml"
                : item == NavigationItem.TABLES ? "/fxml/tables-view.fxml"
                : item == NavigationItem.PRODUCTS ? "/fxml/menu-management-view.fxml"
                : item == NavigationItem.INVENTORY ? "/fxml/inventory-management-view.fxml"
                : item == NavigationItem.REGISTERS ? "/fxml/register-management-view.fxml"
                : item == NavigationItem.CUSTOMERS ? "/fxml/customer-management-view.fxml"
                : item == NavigationItem.LOYALTY ? "/fxml/loyalty-view.fxml"
                : item == NavigationItem.EMPLOYEES ? "/fxml/employee-management-view.fxml"
                : item == NavigationItem.PURCHASING ? "/fxml/purchasing-view.fxml"
                : item == NavigationItem.EXPENSES ? "/fxml/expenses-view.fxml"
                : item == NavigationItem.DISCOUNTS ? "/fxml/discounts-view.fxml"
                : item == NavigationItem.DOCUMENTS ? "/fxml/documents-view.fxml"
                : item == NavigationItem.REFUNDS ? "/fxml/refunds-view.fxml"
                : item == NavigationItem.NOTIFICATIONS ? "/fxml/notifications-view.fxml"
                : item == NavigationItem.AUDIT_LOG ? "/fxml/audit-view.fxml"
                : item == NavigationItem.REPORTS ? "/fxml/reports-view.fxml"
                : item == NavigationItem.SETTINGS ? "/fxml/settings-view.fxml"
                : "/fxml/module-placeholder.fxml";
        FXMLLoader loader = new FXMLLoader(resource(fxml));
        loader.setControllerFactory(type -> {
            if (type == DashboardController.class) {
                return new DashboardController(new DashboardService(
                        new JdbcDashboardRepository(applicationContext.dataSource())),
                        destination -> mainShellController.navigateTo(destination));
            }
            if (type == ModulePlaceholderController.class) {
                return new ModulePlaceholderController(item.label());
            }
            if (type == PosController.class) {
                return new PosController(new PosService(new JdbcPosRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == MenuManagementController.class) {
                return new MenuManagementController(new MenuManagementService(
                        new JdbcMenuManagementRepository(applicationContext.dataSource())));
            }
            if (type == InventoryManagementController.class) {
                return new InventoryManagementController(new InventoryService(
                        new JdbcInventoryRepository(applicationContext.dataSource())),applicationContext.session());
            }
            if (type == RegisterManagementController.class) {
                return new RegisterManagementController(new RegisterService(
                        new JdbcRegisterRepository(applicationContext.dataSource())), applicationContext.session());
            }
            OrderManagementService orderService = new OrderManagementService(
                    new JdbcOrderManagementRepository(applicationContext.dataSource()));
            if (type == OrdersController.class) {
                return new OrdersController(orderService,
                        new CheckoutService(new JdbcCheckoutRepository(applicationContext.dataSource())),
                        new OrderDiscountService(new JdbcOrderDiscountRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == TablesController.class) {
                return new TablesController(new TableManagementService(
                        new JdbcTableManagementRepository(applicationContext.dataSource())),
                        destination -> mainShellController.navigateTo(destination));
            }
            if (type == ReservationsController.class) {
                return new ReservationsController(new TableManagementService(
                        new JdbcTableManagementRepository(applicationContext.dataSource())),
                        new CustomerService(new JdbcCustomerRepository(applicationContext.dataSource())));
            }
            if (type == CustomerManagementController.class) {
                return new CustomerManagementController(new CustomerService(
                        new JdbcCustomerRepository(applicationContext.dataSource())));
            }
            if (type == LoyaltyController.class) {
                return new LoyaltyController(new LoyaltyService(
                        new JdbcLoyaltyRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == EmployeeManagementController.class) {
                return new EmployeeManagementController(new EmployeeService(
                        new JdbcEmployeeRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == PurchasingController.class) {
                return new PurchasingController(new PurchasingService(
                        new JdbcPurchasingRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == ExpenseController.class) {
                return new ExpenseController(new ExpenseService(
                        new JdbcExpenseRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == ReportsController.class) {
                return new ReportsController(new ReportService(
                        new JdbcReportRepository(applicationContext.dataSource())));
            }
            if (type == SettingsController.class) {
                return new SettingsController(new SettingService(
                        new JdbcSettingRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == DiscountController.class) {
                return new DiscountController(new DiscountService(
                        new JdbcDiscountRepository(applicationContext.dataSource())));
            }
            if (type == DocumentsController.class) {
                return new DocumentsController(new DocumentService(
                        new JdbcDocumentRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == NotificationsController.class) {
                return new NotificationsController(new NotificationService(
                        new JdbcNotificationRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            if (type == AuditController.class) {
                return new AuditController(new AuditService(
                        new JdbcAuditRepository(applicationContext.dataSource())));
            }
            if (type == RefundController.class) {
                return new RefundController(new RefundService(
                        new JdbcRefundRepository(applicationContext.dataSource())),
                        applicationContext.session());
            }
            throw unsupportedController(type);
        });
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Module view could not be loaded.", exception);
        }
    }

    private void logoutToLogin() {
        mainShellController = null;
        try {
            showLogin();
        } catch (IOException exception) {
            LOGGER.error("Login view could not be restored ({}).", exception.getClass().getSimpleName());
            showNavigationFailure();
        }
    }

    private void closeLoginController() {
        if (loginController != null) {
            loginController.close();
            loginController = null;
        }
    }

    private static URL resource(String path) {
        return Objects.requireNonNull(CoffeeShopApplication.class.getResource(path),
                "Missing required resource: " + path);
    }

    private static IllegalArgumentException unsupportedController(Class<?> type) {
        return new IllegalArgumentException("Unsupported FXML controller: " + type.getName());
    }

    private static void showNavigationFailure() {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                "The requested screen could not be opened. Please restart the application.");
        alert.setHeaderText("Unable to open screen");
        alert.showAndWait();
    }

    @Override
    public void stop() {
        closeLoginController();
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
