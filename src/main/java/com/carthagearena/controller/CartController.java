package com.carthagearena.controller;

import com.carthagearena.model.CartItem;
import com.carthagearena.service.CartService;
import com.carthagearena.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la vue du panier
 */
public class CartController implements Initializable {

    @FXML private ListView<CartItem> lvCartItems;
    @FXML private Label lblSubtotal;
    @FXML private Label lblTotal;
    @FXML private Button btnCheckout;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();
        updateSummary();
        
        CartService.getInstance().getItems().addListener((javafx.collections.ListChangeListener<CartItem>) c -> {
            updateSummary();
        });
    }

    private void setupListView() {
        lvCartItems.setItems(CartService.getInstance().getItems());
        lvCartItems.setCellFactory(param -> new CartItemCell());
    }

    private void updateSummary() {
        String total = CartService.getInstance().getTotalFormatted();
        lblSubtotal.setText(total);
        lblTotal.setText(total);
        btnCheckout.setDisable(CartService.getInstance().getItems().isEmpty());
    }

    @FXML
    private void onBackToShop() {
        loadMainView("/fxml/ShopView.fxml");
    }

    @FXML
    private void onClearCart() {
        CartService.getInstance().clear();
    }

    @FXML
    private void onCheckout() {
        if (AuthService.getInstance().isLoggedIn()) {
            loadMainView("/fxml/PaymentView.fxml");
        } else {
            loadMainView("/fxml/LoginView.fxml");
        }
    }

    private void loadMainView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            
            // On délègue au contrôleur principal pour afficher la vue
            if (fxml.contains("Shop")) controller.showShop();
            else if (fxml.contains("Payment")) controller.showPayment();
            else if (fxml.contains("Login")) controller.showLogin();
            
            lvCartItems.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cellule personnalisée pour afficher un article du panier
     */
    private static class CartItemCell extends ListCell<CartItem> {
        @Override
        protected void updateItem(CartItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                HBox box = new HBox(15);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(10));

                Label name = new Label(item.getProduct().getName());
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                
                Label price = new Label(item.getProduct().getPriceFormatted());
                price.setStyle("-fx-text-fill: #8b949e;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label qty = new Label("Qté: " + item.getQuantity());
                
                Label total = new Label(item.getTotalPriceFormatted());
                total.setStyle("-fx-text-fill: #e6a817; -fx-font-weight: bold;");

                Button btnDelete = new Button("❌");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> CartService.getInstance().removeProduct(item.getProduct()));

                box.getChildren().addAll(name, price, spacer, qty, total, btnDelete);
                setGraphic(box);
            }
        }
    }
}
