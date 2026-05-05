package com.carthagearena.controller;

import com.carthagearena.service.AuthService;
import com.carthagearena.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Contrôleur pour la page de login (Admin et Client)
 */
public class LoginController {

    @FXML private TextField tfUsername; // Utilisé comme Email
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;

    @FXML
    private void onLogin() {
        String email = tfUsername.getText();
        String pass = pfPassword.getText();

        if (AuthService.getInstance().login(email, pass)) {
            User user = AuthService.getInstance().getCurrentUser();
            if (user.isAdmin()) {
                loadMainDashboard(); // Vue Admin
            } else {
                loadMainView(); // Vue Boutique Client
            }
        } else {
            lblError.setText("Identifiants incorrects ❌");
            lblError.setVisible(true);
        }
    }

    @FXML
    private void onBackToShop() {
        loadMainView();
    }

    private void loadMainDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            
            // On affiche directement la gestion merch (admin)
            controller.showMerch();
            
            tfUsername.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            // showShop est appelé par défaut dans initialize de MainController
            tfUsername.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
