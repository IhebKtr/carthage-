package com.carthage.controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class AdminMainLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox sidebarMenu;

    @FXML
    public void initialize() {
        // Load the initial view, e.g. Dashboard or Games
        // loadView("/com/carthage/view/admin/games-view.fxml");
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            // Save the controller so children nodes can access it
            contentArea.setUserData(this);
        } catch (IOException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Failed to load view: " + fxmlPath);
            errorLabel.setStyle("-fx-text-fill: red;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    public <T> T loadViewAndGetController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            contentArea.setUserData(this);
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void showDashboard() {
        // loadView("/com/carthage/view/admin/dashboard-view.fxml");
    }

    @FXML
    private void showUtilisateurs() {
        // loadView("/com/carthage/view/admin/users-view.fxml");
    }

    @FXML
    private void showReclamations() {
        loadView("/com/carthage/view/admin/reclamations-view.fxml");
    }

    @FXML
    private void showTournois() {
        loadView("/com/carthage/view/admin/tournois-view.fxml");
    }

    @FXML
    private void showJeux() {
        loadView("/com/carthage/view/admin/games-view.fxml");
    }

    @FXML
    public void onLogoutClicked() {
        com.carthage.utils.SessionContext.getInstance().cleanSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/login-view.fxml"));
            Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
            stage.setTitle("Carthage Arena – Connexion");
            stage.setScene(new javafx.scene.Scene(root, 1100, 700));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
