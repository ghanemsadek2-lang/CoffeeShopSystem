package com.coffeeshop.controller;
import com.coffeeshop.model.*;import com.coffeeshop.service.OrderManagementService;import javafx.application.Platform;import javafx.fxml.FXML;import javafx.scene.control.*;import javafx.scene.layout.FlowPane;import java.util.concurrent.CompletableFuture;import java.util.function.Consumer;
public final class TablesController{
 private final OrderManagementService service;private final Consumer<NavigationItem> navigator;@FXML private FlowPane tablePane;@FXML private Label messageLabel;
 public TablesController(OrderManagementService s,Consumer<NavigationItem> n){service=s;navigator=n;}
 @FXML private void initialize(){refresh();}@FXML private void refresh(){CompletableFuture.supplyAsync(service::tables).whenComplete((v,e)->Platform.runLater(()->{tablePane.getChildren().clear();if(e!=null){messageLabel.setText("Unable to load tables.");return;}if(v.isEmpty()){messageLabel.setText("No active cafe tables are configured.");return;}messageLabel.setText("");v.forEach(this::addTable);}));}
 private void addTable(CafeTableInfo t){Button b=new Button(t.name()+"\n"+t.status()+" · "+t.capacity()+" seats");b.getStyleClass().addAll("table-card","table-"+t.status().toLowerCase());b.setOnAction(e->navigator.accept("AVAILABLE".equals(t.status())?NavigationItem.POS:NavigationItem.ORDERS));tablePane.getChildren().add(b);}
}
