package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Order;
import com.carthagearena.model.User;
import com.carthagearena.service.AuthService;
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
import java.util.UUID;

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
    @FXML private VBox  panelHistory;

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
    private final com.carthagearena.service.AiService aiService = new com.carthagearena.service.AiService();
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

        // Gestion de l'historique : masqué pour les clients
        boolean isAdmin = AuthService.getInstance().isAdmin();
        panelHistory.setVisible(isAdmin);
        panelHistory.setManaged(isAdmin);

        // Récupération automatique des données client
        if (AuthService.getInstance().isLoggedIn()) {
            User user = AuthService.getInstance().getCurrentUser();
            tfUserId.setText(user.getId().toString());
            tfUserEmail.setText(user.getEmail());
            
            // Empêcher la modification si connecté
            tfUserId.setEditable(false);
            tfUserEmail.setEditable(false);
            tfUserId.setStyle("-fx-opacity: 0.8; -fx-background-color: #1c2128;");
            tfUserEmail.setStyle("-fx-opacity: 0.8; -fx-background-color: #1c2128;");
            
            // Charger l'historique seulement si visible (Admin)
            if (isAdmin) {
                loadUserOrders(user.getId());
            }
        }

        // Intégration Panier E-commerce
        if (com.carthagearena.service.CartService.getInstance().getItemCount() > 0) {
            setupCheckoutFromCart();
        }
    }

    private void setupCheckoutFromCart() {
        com.carthagearena.service.CartService cart = com.carthagearena.service.CartService.getInstance();
        panelSummary.setVisible(true);
        lblProductName.setText(cart.getItems().size() + " types d'articles");
        lblProductPrice.setText("Total du panier");
        lblTotalAmount.setText(cart.getTotalFormatted());

        // Calcul du score de fraude basé sur la quantité totale et le montant total
        int totalQty    = cart.getItems().stream().mapToInt(com.carthagearena.model.CartItem::getQuantity).sum();
        int totalAmount = cart.getTotalCents(); // en centimes
        double score = aiService.fraudScore(totalQty, totalAmount);
        lblFraudScore.setText(aiService.fraudScoreLabel(score));
        if (score >= 0.8) {
            lblFraudScore.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
        } else if (score >= 0.5) {
            lblFraudScore.setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
        } else {
            lblFraudScore.setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
        }

        // On désactive la sélection manuelle si on vient du panier
        cbMerch.setDisable(true);
        tfQuantity.setDisable(true);
        lblStatus.setText("🛒 Prêt pour le paiement des articles du panier.");
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
            double score = aiService.fraudScore(qty, total);
            lblFraudScore.setText(aiService.fraudScoreLabel(score));
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

        com.carthagearena.service.CartService cart = com.carthagearena.service.CartService.getInstance();
        boolean fromCart = !cart.getItems().isEmpty();

        UUID userId; String email;
        try {
            userId = UUID.fromString(tfUserId.getText().trim());
            email  = tfUserEmail.getText().trim();
        } catch (IllegalArgumentException e) {
            showError("UserId invalide", "L'identifiant utilisateur doit être un UUID valide.");
            return;
        }

        setLoading(true);
        lblStatus.setText("⏳ Création de la session Stripe...");

        Thread thread = new Thread(() -> {
            try {
                String checkoutUrl;
                Order order = new Order(userId, email);

                if (fromCart) {
                    // Commande multi-articles depuis le panier
                    for (com.carthagearena.model.CartItem item : cart.getItems()) {
                        order.addItem(item.getProduct(), item.getQuantity());
                    }
                    // Note: PaymentService may need update for multi-item sessions, 
                    // using first item as primary reference for now or a generic "Panier"
                    checkoutUrl = paymentService.createCheckoutSession(cart.getItems().get(0).getProduct(), userId, email);
                } else {
                    // Commande produit unique
                    Merch merch = cbMerch.getValue();
                    int qty = Integer.parseInt(tfQuantity.getText().trim());
                    order.addItem(merch, qty);
                    checkoutUrl = paymentService.createCheckoutSession(merch, userId, email);
                }

                orderService.createOrder(order);

                Platform.runLater(() -> {
                    lblStatus.setText("✅ Session Stripe créée ! Ouverture du navigateur...");
                    setLoading(false);
                    openBrowser(checkoutUrl);
                    if (fromCart) cart.clear();
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

        com.carthagearena.service.CartService cart = com.carthagearena.service.CartService.getInstance();
        boolean fromCart = !cart.getItems().isEmpty();
        
        UUID userId = UUID.fromString(tfUserId.getText().trim());
        String email = tfUserEmail.getText().trim();

        try {
            Order order = new Order(userId, email);
            
            if (fromCart) {
                for (com.carthagearena.model.CartItem item : cart.getItems()) {
                    order.addItem(item.getProduct(), item.getQuantity());
                }
            } else {
                Merch merch = cbMerch.getValue();
                int qty = Integer.parseInt(tfQuantity.getText().trim());
                order.addItem(merch, qty);
            }

            orderService.createOrder(order);
            paymentService.simulatePayment(order.getId().toString());

            if (fromCart) cart.clear();

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
            loadUserOrders(UUID.fromString(userIdStr));
        } catch (IllegalArgumentException e) {
            showError("UserId invalide", "Entrez un UUID valide.");
        }
    }

    private void loadUserOrders(UUID userId) {
        try {
            List<Order> orders = orderService.findByUserId(userId);
            tableUserOrders.setItems(FXCollections.observableArrayList(orders));
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean fromCart = !com.carthagearena.service.CartService.getInstance().getItems().isEmpty();
        
        if (!fromCart && cbMerch.getValue() == null) { 
            showError("Produit requis", "Sélectionnez un produit."); return false; 
        }
        if (tfUserId.getText().isBlank() || tfUserEmail.getText().isBlank()) {
            showError("Champs requis", "Remplissez l'ID et l'email utilisateur."); return false;
        }
        if (!fromCart && tfQuantity.getText().isBlank()) {
            showError("Quantité requise", "Entrez une quantité."); return false;
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
