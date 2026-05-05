package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.service.CartService;
import com.carthagearena.service.MerchService;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la boutique e-commerce
 */
public class ShopController implements Initializable {

    @FXML private FlowPane gridProducts;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblCartCount;
    @FXML private Label lblCartTotal;
    @FXML private ScrollPane scrollPane;

    private final MerchService merchService = new MerchService();
    private List<Merch> allProducts;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupCartListener();
        loadCategories();
        loadProducts();

        tfSearch.textProperty().addListener((obs, oldV, newV) -> filterProducts());
        cbCategory.valueProperty().addListener((obs, oldV, newV) -> filterProducts());
    }

    private void setupCartListener() {
        CartService cart = CartService.getInstance();
        updateCartDisplay();
        
        cart.getItems().addListener((ListChangeListener<Object>) c -> {
            Platform.runLater(this::updateCartDisplay);
        });
    }

    private void updateCartDisplay() {
        CartService cart = CartService.getInstance();
        lblCartCount.setText(String.valueOf(cart.getItemCount()));
        lblCartTotal.setText(cart.getTotalFormatted());
    }

    private void loadCategories() {
        cbCategory.getItems().add("Toutes");
        // Ces noms doivent correspondre aux types enregistrés en base de données
        cbCategory.getItems().addAll("shirt", "cap", "jersey", "poster");
        cbCategory.getSelectionModel().selectFirst();
    }

    private void loadProducts() {
        try {
            allProducts = merchService.findAll();
            displayProducts(allProducts);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayProducts(List<Merch> products) {
        gridProducts.getChildren().clear();
        for (Merch p : products) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductCard.fxml"));
                Parent card = loader.load();
                ProductCardController controller = loader.getController();
                controller.setData(p);
                gridProducts.getChildren().add(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void filterProducts() {
        if (allProducts == null) return;

        String query = tfSearch.getText() != null ? tfSearch.getText().toLowerCase().trim() : "";
        String cat = cbCategory.getValue();

        List<Merch> filtered = allProducts.stream()
                .filter(p -> {
                    boolean matchesName = p.getName() != null && p.getName().toLowerCase().contains(query);
                    boolean matchesDesc = p.getDescription() != null && p.getDescription().toLowerCase().contains(query);
                    return matchesName || matchesDesc;
                })
                .filter(p -> "Toutes".equals(cat) || (p.getType() != null && p.getType().equalsIgnoreCase(cat)))
                .collect(Collectors.toList());

        displayProducts(filtered);
    }

    @FXML
    private void onViewCart() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/fxml/CartView.fxml"));
            // On accède à l'aire de contenu via la scène
            StackPane contentArea = (StackPane) scrollPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
