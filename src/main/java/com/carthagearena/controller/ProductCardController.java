package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.service.CartService;
import com.carthagearena.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Contrôleur pour une carte produit individuelle
 */
public class ProductCardController {

    @FXML private ImageView ivProduct;
    @FXML private Label lblName;
    @FXML private Label lblType;
    @FXML private Label lblPrice;
    @FXML private Label lblDescription;
    @FXML private Button btnAddCart;
    @FXML private Button btnEdit;

    private Merch product;

    /**
     * Remplit la carte avec les données d'un produit
     */
    public void setData(Merch product) {
        this.product = product;
        lblName.setText(product.getName());
        lblType.setText(product.getType().toUpperCase());
        lblPrice.setText(product.getPriceFormatted());
        lblDescription.setText(product.getDescription());

        loadProductImage(product.getImageUrl());

        // Logique de rôle : Admin vs Client/Visiteur
        boolean isAdmin = AuthService.getInstance().isAdmin();
        
        btnEdit.setVisible(isAdmin);
        btnEdit.setManaged(isAdmin);
        
        btnAddCart.setVisible(!isAdmin);
        btnAddCart.setManaged(!isAdmin);

        if (!product.isInStock() && !isAdmin) {
            btnAddCart.setDisable(true);
            btnAddCart.setText("ÉPUISÉ");
            btnAddCart.getStyleClass().add("btn-out-of-stock");
        }
    }

    private void loadProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        try {
            String source = imageUrl.trim();
            if (source.startsWith("data:image")) {
                int commaIndex = source.indexOf(",");
                if (commaIndex != -1) {
                    String base64Data = source.substring(commaIndex + 1);
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                    ivProduct.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes)));
                }
            } else {
                if (!source.startsWith("http") && !source.startsWith("file:")) {
                    source = "file:" + source;
                }
                ivProduct.setImage(new javafx.scene.image.Image(source, true));
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image : " + imageUrl);
        }
    }

    @FXML
    private void onAddToCart() {
        CartService.getInstance().addProduct(product);
    }

    @FXML
    private void onEditProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MerchFormView.fxml"));
            Parent root = loader.load();
            
            MerchFormController controller = loader.getController();
            // On utilise initData comme dans MerchListController
            // Le callback de sauvegarde recharge les données de la boutique
            controller.initData(product, () -> {
                // On pourrait rafraîchir la carte ici si nécessaire,
                // ou simplement laisser l'utilisateur fermer et rouvrir
                // Mais idéalement on recharge la boutique
                try {
                    FXMLLoader shopLoader = new FXMLLoader(getClass().getResource("/fxml/ShopView.fxml"));
                    Parent shopView = shopLoader.load();
                    StackPane contentArea = (StackPane) lblName.getScene().lookup("#contentArea");
                    if (contentArea != null) contentArea.getChildren().setAll(shopView);
                } catch (Exception e) { e.printStackTrace(); }
            });

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("✏️ Modifier le produit");
            stage.setScene(new javafx.scene.Scene(root));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
