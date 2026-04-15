package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Order;
import com.carthagearena.model.OrderItem;
import com.carthagearena.service.MerchService;
import com.carthagearena.service.OrderService;
import com.carthagearena.util.OrderValidator;
import com.carthagearena.util.OrderValidator.ValidationResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur de gestion des commandes (Order)
 * Affiche la liste des commandes avec possibilité de payer/annuler
 */
public class OrderController implements Initializable {

    // ─── Liste des commandes ─────────────────────────────────────────────────
    @FXML private TableView<Order> tableOrders;
    @FXML private TableColumn<Order, String> colOrderId;
    @FXML private TableColumn<Order, String> colOrderDate;
    @FXML private TableColumn<Order, String> colOrderUser;
    @FXML private TableColumn<Order, String> colOrderTotal;
    @FXML private TableColumn<Order, String> colOrderStatus;
    @FXML private TableColumn<Order, Void>   colOrderActions;

    // ─── Détail commande ─────────────────────────────────────────────────────
    @FXML private TableView<OrderItem> tableItems;
    @FXML private TableColumn<OrderItem, String> colItemName;
    @FXML private TableColumn<OrderItem, Integer> colItemQty;
    @FXML private TableColumn<OrderItem, String> colItemUnit;
    @FXML private TableColumn<OrderItem, String> colItemSubtotal;

    // ─── Stats ───────────────────────────────────────────────────────────────
    @FXML private Label lblTotalOrders;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblPendingOrders;

    // ─── Filtre statut ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbStatusFilter;

    private final OrderService orderService   = new OrderService();
    private ObservableList<Order> masterList  = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupOrdersTable();
        setupItemsTable();
        setupFilters();
        loadOrders();
    }

    // ─── Configuration tableaux ───────────────────────────────────────────────

    private void setupOrdersTable() {
        colOrderId.setCellValueFactory(data ->
                new SimpleStringProperty("#" + data.getValue().getId().toString().substring(0, 8)));

        colOrderDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate().toString().replace("T", " ").substring(0, 16)));

        colOrderUser.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUserFullName()));

        colOrderTotal.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTotalFormatted()));

        // Statut coloré
        colOrderStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().getLabel()));
        colOrderStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Payée"     -> setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                        case "Annulée"   -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "En attente"-> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Actions : Payer / Annuler / Voir détail
        colOrderActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPay    = new Button("💳 Payer");
            private final Button btnCancel = new Button("❌ Annuler");

            {
                btnPay.getStyleClass().addAll("btn", "btn-success");
                btnCancel.getStyleClass().addAll("btn", "btn-danger");

                btnPay.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    payOrder(order);
                });
                btnCancel.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    cancelOrder(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Order order = getTableView().getItems().get(getIndex());
                    HBox box = new HBox(8);
                    if (order.canBePaid())     box.getChildren().add(btnPay);
                    if (order.canBeCancelled()) box.getChildren().add(btnCancel);
                    setGraphic(box);
                }
            }
        });

        // Sélection → afficher les items
        tableOrders.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) loadOrderItems(selected);
        });
    }

    private void setupItemsTable() {
        colItemName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMerch().getName()));
        colItemQty.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        colItemUnit.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUnitPriceFormatted()));
        colItemSubtotal.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSubtotalFormatted()));
    }

    private void setupFilters() {
        cbStatusFilter.setItems(FXCollections.observableArrayList(
                "Toutes", "PENDING", "PAID", "CANCELLED"));
        cbStatusFilter.setValue("Toutes");
        cbStatusFilter.valueProperty().addListener((obs, old, val) -> applyFilter());
    }

    private void applyFilter() {
        String filter = cbStatusFilter.getValue();
        ObservableList<Order> filtered = masterList.filtered(o ->
                "Toutes".equals(filter) || o.getStatus().name().equals(filter));
        tableOrders.setItems(filtered);
    }

    // ─── Chargement données ───────────────────────────────────────────────────

    private void loadOrders() {
        try {
            List<Order> orders = orderService.findAll();
            masterList = FXCollections.observableArrayList(orders);
            tableOrders.setItems(masterList);
            updateStats();
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void loadOrderItems(Order order) {
        try {
            Order detailed = orderService.findById(order.getId().toString());
            if (detailed != null) {
                tableItems.setItems(FXCollections.observableArrayList(detailed.getItems()));
            }
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    private void updateStats() {
        lblTotalOrders.setText("Total commandes : " + masterList.size());

        int revenue = masterList.stream()
                .filter(o -> o.getStatus() == Order.Status.PAID)
                .mapToInt(Order::getTotalAmount).sum();
        lblTotalRevenue.setText(String.format("Revenus : %.2f DT", revenue / 100.0));

        long pending = masterList.stream()
                .filter(o -> o.getStatus() == Order.Status.PENDING).count();
        lblPendingOrders.setText("En attente : " + pending);
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    private void payOrder(Order order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer le paiement");
        alert.setHeaderText("Marquer la commande comme payée ?");
        alert.setContentText("Commande : " + order.getId().toString().substring(0, 8)
                + "\nTotal : " + order.getTotalFormatted());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                orderService.payOrder(order.getId().toString());
                loadOrders();
                showInfo("Succès", "Commande marquée comme payée ✅");
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    private void cancelOrder(Order order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer l'annulation");
        alert.setHeaderText("Annuler cette commande ?");
        alert.setContentText("Commande : " + order.getId().toString().substring(0, 8));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                orderService.cancelOrder(order.getId().toString());
                loadOrders();
                showInfo("Succès", "Commande annulée");
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void onRefresh() { loadOrders(); }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
