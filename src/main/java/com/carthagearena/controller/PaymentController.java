package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Order;
import com.carthagearena.service.MerchService;
import com.carthagearena.service.OrderService;
import com.carthagearena.service.PaymentService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur Paiement Stripe - traduit depuis PaymentService.php (Symfony)
 *
 * Fonctionnalités :
 *  - Sélectionner un produit Merch
 *  - Créer une session Stripe Checkout → ouvre le navigateur
 *  - Simuler un paiement en mode test (sans navigateur)
 *  - Voir le score de fraude avant paiement
 */
public class PaymentController implements Initializable {

    // ─── Sélection produit ───────────────────────────────────────────────────
    @FXML private ComboBox<Merch>  cbMerch;
    @FXML private TextField        tfUserId;
    @FXML private TextField        tfUserEmail;
    @FXML private TextField        tfQuantity;

    // ─── Résumé de la commande ────────────────────────────────────────────────
    @FXML private Label lblProductName;
    @FXML private Label lblProductPrice;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblFraudScore;
    @FXML private VBox  panelSummary;

    // ─── Actions ─────────────────────────────────────────────────────────────
    @FXML private Button         btnCheckout;
    @FXML private Button         btnSimulate;
    @FXML private ProgressIndicator spinner;
    @FXML private Label          lblStatus;

    // ─── Historique commandes user ────────────────────────────────────────────
    @FXML private TableView<Order> tableUserOrders;
    @FXML private TableColumn<Order, String> colOrderRef;
    @FXML private TableColumn<Order, String> colOrderTotal;
    @FXML private TableColumn<Order, String> colOrderStatus;

    private final MerchService   merchService  = new MerchService();
    private final OrderService   orderService  = new OrderService();
    private PaymentService       paymentService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            paymentService = new PaymentService(orderService);
        } catch (Exception e) {
            lblStatus.setText("⚠️ Stripe non configuré : " + e.getMessage());
        }

        loadMerchs();
        setupSummaryUpdate();
        setupOrdersTable();
        spinner.setVisible(false);
        panelSummary.setVisible(false);
    }

    // ─── Chargement des produits ──────────────────────────────────────────────

    private void loadMerchs() {
        try {
            List<Merch> list = merchService.findAll();
            cbMerch.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    // ─── Mise à jour du résumé en temps réel ─────────────────────────────────

    private void setupSummaryUpdate() {
        cbMerch.valueProperty().addListener((obs, old, m) -> updateSummary());
        tfQuantity.textProperty().addListener((obs, old, v) -> updateSummary());
    }

    private void updateSummary() {
        Merch merch = cbMerch.getValue();
        String qtyStr = tfQuantity.getText().trim();

        if (merch == null || qtyStr.isBlank()) {
            panelSummary.setVisible(false);
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0) { panelSummary.setVisible(false); return; }

            int total = merch.getPrice() * qty;

            lblProductName.setText(merch.getName());
            lblProductPrice.setText(merch.getPriceFormatted() + " × " + qty);
            lblTotalAmount.setText(String.format("%.2f DT", total / 100.0));

            // Score fraude (AiService)
            com.carthagearena.service.AiService ai = new com.carthagearena.service.AiService();
            double score = ai.fraudScore(qty, total);
            lblFraudScore.setText(ai.fraudScoreLabel(score));
            if (score >= 0.8) {
                lblFraudScore.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
            } else if (score >= 0.5) {
                lblFraudScore.setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
            } else {
                lblFraudScore.setStyle("-fx-text-fill: #3fb950;");
            }

            panelSummary.setVisible(true);

        } catch (NumberFormatException ignored) {
            panelSummary.setVisible(false);
        }
    }

    // ─── Stripe Checkout (ouvre navigateur) ──────────────────────────────────

    @FXML
    private void onCheckout() {
        if (!validateForm()) return;

        Merch merch = cbMerch.getValue();
        int userId; String email;
        try {
            userId = Integer.parseInt(tfUserId.getText().trim());
            email  = tfUserEmail.getText().trim();
        } catch (NumberFormatException e) {
            showError("UserId invalide", "L'identifiant utilisateur doit être un entier.");
            return;
        }

        setLoading(true);
        lblStatus.setText("⏳ Création de la session Stripe...");

        Thread thread = new Thread(() -> {
            try {
                // Créer la session Stripe (équiv. PHP : createCheckoutSession(...))
                String checkoutUrl = paymentService.createCheckoutSession(merch, userId, email);

                // Créer la commande en BDD avec statut PENDING
                Order order = new Order(userId, email);
                int qty = Integer.parseInt(tfQuantity.getText().trim());
                order.addItem(merch, qty);
                orderService.createOrder(order);

                // Ouvrir le navigateur
                Platform.runLater(() -> {
                    lblStatus.setText("✅ Session Stripe créée ! Ouverture du navigateur...");
                    setLoading(false);
                    openBrowser(checkoutUrl);
                    loadUserOrders(userId);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Erreur Stripe : " + e.getMessage());
                    setLoading(false);
                    showError("Erreur Stripe", e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ─── Simulation paiement (mode test) ─────────────────────────────────────

    @FXML
    private void onSimulatePayment() {
        if (!validateForm()) return;

        Merch merch = cbMerch.getValue();
        int userId = Integer.parseInt(tfUserId.getText().trim());
        String email = tfUserEmail.getText().trim();
        int qty = Integer.parseInt(tfQuantity.getText().trim());

        try {
            // Créer la commande PENDING
            Order order = new Order(userId, email);
            order.addItem(merch, qty);
            orderService.createOrder(order);

            // Simuler le paiement → PAID immédiatement
            paymentService.simulatePayment(order.getId().toString());

            lblStatus.setText("✅ [Mode test] Commande " +
                    order.getId().toString().substring(0, 8) + " payée !");

            showInfo("Paiement simulé ✅",
                    "La commande a été créée et marquée comme PAID.\n" +
                    "Total : " + order.getTotalFormatted());

            loadUserOrders(userId);

        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    // ─── Historique commandes ─────────────────────────────────────────────────

    private void setupOrdersTable() {
        colOrderRef.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        "#" + data.getValue().getId().toString().substring(0, 8)));
        colOrderTotal.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTotalFormatted()));
        colOrderStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatus().getLabel()));

        // Couleur statut
        colOrderStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                setStyle(switch (status) {
                    case "Payée"      -> "-fx-text-fill: #3fb950; -fx-font-weight: bold;";
                    case "Annulée"    -> "-fx-text-fill: #f85149;";
                    default           -> "-fx-text-fill: #d29922;";
                });
            }
        });
    }

    @FXML
    private void onLoadHistory() {
        String userIdStr = tfUserId.getText().trim();
        if (userIdStr.isBlank()) { showError("UserId requis", "Entrez un ID utilisateur."); return; }
        try {
            loadUserOrders(Integer.parseInt(userIdStr));
        } catch (NumberFormatException e) {
            showError("UserId invalide", "Entrez un entier valide.");
        }
    }

    private void loadUserOrders(int userId) {
        try {
            List<Order> orders = orderService.findByUserId(userId);
            tableUserOrders.setItems(FXCollections.observableArrayList(orders));
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private boolean validateForm() {
        Merch merch = cbMerch.getValue();
        if (merch == null) { showError("Produit requis", "Sélectionnez un produit."); return false; }
        if (tfUserId.getText().isBlank() || tfUserEmail.getText().isBlank()) {
            showError("Champs requis", "Remplissez l'ID et l'email utilisateur."); return false;
        }
        if (tfQuantity.getText().isBlank()) {
            showError("Quantité requise", "Entrez une quantité."); return false;
        }
        try { Integer.parseInt(tfQuantity.getText()); } catch (NumberFormatException e) {
            showError("Quantité invalide", "La quantité doit être un entier."); return false;
        }
        return true;
    }

    private void setLoading(boolean loading) {
        spinner.setVisible(loading);
        btnCheckout.setDisable(loading);
        btnSimulate.setDisable(loading);
    }

    private void openBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            showInfo("URL Stripe", "Ouvrez ce lien dans votre navigateur :\n" + url);
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
