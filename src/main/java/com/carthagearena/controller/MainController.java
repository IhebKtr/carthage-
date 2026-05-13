package com.carthagearena.controller;

import com.carthagearena.service.AuthService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal - gère la navigation entre les vues
 */
public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnShop;
    @FXML private Button btnMerch;
    @FXML private Button btnOrders;
    @FXML private Button btnAi;
    @FXML private Button btnPayment;
    @FXML private Button btnStats;
    @FXML private Button btnLogout;
    @FXML private Button btnLogin;
    @FXML private Label lblUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        showShop(); // Boutique par défaut
        updateAuthUI();
    }

    private void updateAuthUI() {
        boolean loggedIn = AuthService.getInstance().isLoggedIn();
        boolean isAdmin = AuthService.getInstance().isAdmin();

        // Visibilité des boutons Admin
        btnMerch.setVisible(isAdmin);
        btnMerch.setManaged(isAdmin);
        btnOrders.setVisible(isAdmin);
        btnOrders.setManaged(isAdmin);
        btnAi.setVisible(isAdmin);
        btnAi.setManaged(isAdmin);
        btnStats.setVisible(isAdmin);
        btnStats.setManaged(isAdmin);

        // Login / Logout / User info
        btnLogin.setVisible(!loggedIn);
        btnLogin.setManaged(!loggedIn);
        
        btnLogout.setVisible(loggedIn);
        btnLogout.setManaged(loggedIn);
        
        if (loggedIn) {
            lblUser.setText("👤 " + AuthService.getInstance().getCurrentUser().getFullName());
        } else {
            lblUser.setText("👤 Mode Visiteur");
        }
        lblUser.setVisible(true);
        lblUser.setManaged(true);
    }

    @FXML
    public void showShop() {
        loadView("/fxml/ShopView.fxml");
        setActiveButton(btnShop);
    }

    @FXML
    public void showMerch() {
        if (checkAdmin()) {
            loadView("/fxml/MerchListView.fxml");
            setActiveButton(btnMerch);
        }
    }

    @FXML
    public void showOrders() {
        if (checkAdmin()) {
            loadView("/fxml/OrderView.fxml");
            setActiveButton(btnOrders);
        }
    }

    @FXML
    public void showAi() {
        if (checkAdmin()) {
            loadView("/fxml/AiDescriptionView.fxml");
            setActiveButton(btnAi);
        }
    }

    @FXML
    public void showPayment() {
        loadView("/fxml/PaymentView.fxml");
        setActiveButton(btnPayment);
    }

    @FXML
    public void showStats() {
        if (checkAdmin()) {
            loadView("/fxml/StatsView.fxml");
            setActiveButton(btnStats);
        }
    }

    @FXML
    public void onLogout() {
        AuthService.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showLogin() {
        loadView("/fxml/LoginView.fxml");
        setActiveButton(btnLogin);
    }

    private boolean checkAdmin() {
        if (AuthService.getInstance().isAdmin()) {
            return true;
        } else {
            loadView("/fxml/LoginView.fxml");
            setActiveButton(null);
            return false;
        }
    }

    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button active) {
        for (Button btn : new Button[]{btnShop, btnMerch, btnOrders, btnAi, btnPayment, btnStats, btnLogin, btnLogout}) {
            if (btn != null) {
                btn.getStyleClass().remove("active");
                btn.getStyleClass().remove("nav-btn-active");
            }
        }
        if (active != null) active.getStyleClass().add("active");
    }
}
