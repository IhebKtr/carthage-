package com.carthagearena.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal - gère la navigation entre les vues
 */
public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnMerch;
    @FXML private Button btnOrders;
    @FXML private Button btnAi;
    @FXML private Button btnPayment;
    @FXML private Button btnStats;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        showMerch(); // Vue par défaut
    }

    @FXML
    public void showMerch() {
        loadView("/fxml/MerchListView.fxml");
        setActiveButton(btnMerch);
    }

    @FXML
    public void showOrders() {
        loadView("/fxml/OrderView.fxml");
        setActiveButton(btnOrders);
    }

    @FXML
    public void showAi() {
        loadView("/fxml/AiDescriptionView.fxml");
        setActiveButton(btnAi);
    }

    @FXML
    public void showPayment() {
        loadView("/fxml/PaymentView.fxml");
        setActiveButton(btnPayment);
    }

    @FXML
    public void showStats() {
        loadView("/fxml/StatsView.fxml");
        setActiveButton(btnStats);
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
        btnMerch.getStyleClass().remove("nav-btn-active");
        btnOrders.getStyleClass().remove("nav-btn-active");
        btnAi.getStyleClass().remove("nav-btn-active");
        btnPayment.getStyleClass().remove("nav-btn-active");
        btnStats.getStyleClass().remove("nav-btn-active");
        active.getStyleClass().add("nav-btn-active");
    }
}
