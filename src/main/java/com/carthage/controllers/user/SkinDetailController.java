package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.DatabaseConnection;
import com.carthage.utils.SessionContext;
import com.carthage.utils.UUIDUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class SkinDetailController {

    @FXML private ImageView skinImage;
    @FXML private Label skinName, skinDesc, skinPrice, gameBadge, rarityBadge;
    @FXML private Button buyButton;
    @FXML private HBox relatedList;

    private Connection connection;
    private UUID currentSkinId;
    private int currentPrice;
    private UUID currentGameId;

    @FXML
    public void initialize() {
        connection = DatabaseConnection.getInstance().getConnection();
    }

    public void setSkin(UUID skinId) {
        this.currentSkinId = skinId;
        loadSkinData();
        loadRelatedItems();
    }

    private void loadSkinData() {
        String sql = "SELECT s.*, g.name as game_name, g.id as game_id FROM skin s " +
                     "JOIN game g ON s.game_id = g.id " +
                     "WHERE s.id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, currentSkinId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                skinName.setText(rs.getString("name"));
                skinDesc.setText(rs.getString("description"));
                currentPrice = rs.getInt("price");
                skinPrice.setText(String.valueOf(currentPrice));
                gameBadge.setText(rs.getString("game_name").toUpperCase());
                String rarity = rs.getString("rarity");
                rarityBadge.setText(rarity != null ? rarity.toUpperCase() : "COMMON");
                currentGameId = UUIDUtils.fromBytes(rs.getBytes("game_id"));

                String imageUrl = rs.getString("image_url");
                if (imageUrl != null && !imageUrl.isBlank()) {
                    try {
                        skinImage.setImage(new Image(imageUrl, true));
                    } catch (Exception e) {
                        // Keep placeholder
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRelatedItems() {
        relatedList.getChildren().clear();
        String sql = "SELECT s.id, s.name, s.price, s.image_url FROM skin s " +
                     "WHERE s.game_id = UNHEX(REPLACE(?, '-', '')) AND s.id != UNHEX(REPLACE(?, '-', '')) " +
                     "LIMIT 4";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, currentGameId.toString());
            ps.setString(2, currentSkinId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID id = UUIDUtils.fromBytes(rs.getBytes("id"));
                relatedList.getChildren().add(buildSmallCard(
                    id,
                    rs.getString("name"),
                    rs.getInt("price"),
                    rs.getString("image_url")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildSmallCard(UUID id, String name, int price, String imgUrl) {
        VBox card = new VBox(10);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12; -fx-cursor: hand;");
        card.setPadding(new Insets(10));
        
        StackPane imgFrame = new StackPane();
        imgFrame.setPrefHeight(100);
        imgFrame.setStyle("-fx-background-color: #1E2633; -fx-background-radius: 8;");
        if (imgUrl != null && !imgUrl.isBlank()) {
            ImageView iv = new ImageView(new Image(imgUrl, true));
            iv.setFitWidth(160); iv.setFitHeight(90); iv.setPreserveRatio(true);
            imgFrame.getChildren().add(iv);
        }
        
        Label n = new Label(name); n.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label p = new Label("💰 " + price); p.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 11px;");
        
        card.getChildren().addAll(imgFrame, n, p);
        card.setOnMouseClicked(e -> {
            MainLayoutController mlc = (MainLayoutController) card.getScene().lookup("#contentArea").getUserData();
            mlc.loadSkinDetail(id);
        });
        return card;
    }

    @FXML
    public void onPurchase() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) return;

        // 1. Get current balance
        int balance = 0;
        String balanceSql = "SELECT balance FROM user WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(balanceSql)) {
            ps.setString(1, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) balance = rs.getInt("balance");
        } catch (SQLException e) { e.printStackTrace(); }

        if (balance < currentPrice) {
            showAlert("Solde insuffisant", "Vous n'avez pas assez de points pour acheter ce skin.");
            return;
        }

        // 2. Process transaction
        try {
            connection.setAutoCommit(false);
            
            // Deduct balance
            String updateSql = "UPDATE user SET balance = balance - ? WHERE id = UNHEX(REPLACE(?, '-', ''))";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, currentPrice);
                ps.setString(2, user.getId().toString());
                ps.executeUpdate();
            }

            // Record skin ownership
            String insertSql = "INSERT INTO user_skin (id, purchased_at, status, user_id, skin_id) " +
                               "VALUES (UNHEX(?), NOW(), 'ACTIVE', UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')))";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
                ps.setString(2, user.getId().toString());
                ps.setString(3, currentSkinId.toString());
                ps.executeUpdate();
            }

            connection.commit();
            showAlert("Succès", "Skin acheté avec succès ! Il est maintenant disponible dans votre profil.");
            
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            showAlert("Erreur", "Une erreur est survenue lors de l'achat.");
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @FXML
    public void onBack() {
        MainLayoutController mlc = (MainLayoutController) skinName.getScene().lookup("#contentArea").getUserData();
        mlc.loadView("boutique-view.fxml");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
