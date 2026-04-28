package com.carthage.controllers.user;

import com.carthage.utils.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.sql.*;

public class BoutiqueController {

    @FXML private FlowPane skinsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> gameCombo;

    private Connection connection;
    private final com.carthage.services.GameService gameService = new com.carthage.services.GameService();

    @FXML
    public void initialize() {
        connection = DatabaseConnection.getInstance().getConnection();
        
        if (gameCombo != null) {
            gameCombo.getItems().add("Tous les Jeux");
            for (com.carthage.entity.Game game : gameService.getAllGames()) {
                gameCombo.getItems().add(game.getName());
            }
            gameCombo.setValue("Tous les Jeux");
            gameCombo.valueProperty().addListener((obs, old, val) -> loadSkins());
        }
        
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Prix: Décroissant", "Prix: Croissant", "Nom: A-Z", "Nom: Z-A");
            sortCombo.setValue("Prix: Décroissant");
            sortCombo.valueProperty().addListener((obs, old, val) -> loadSkins());
        }
        
        loadSkins();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> loadSkins());
        }
    }

    private void loadSkins() {
        skinsGrid.getChildren().clear();
        String search = searchField != null ? searchField.getText() : null;
        boolean hasSearch = search != null && !search.isBlank();
        
        String selectedGame = gameCombo != null ? gameCombo.getValue() : null;
        boolean hasGameFilter = selectedGame != null && !"Tous les Jeux".equals(selectedGame);

        // Doctrine snake_case columns: skin.name, skin.price, skin.rarity, skin.image_url
        // game.name joined via skin.game_id
        StringBuilder sql = new StringBuilder(
            "SELECT s.id, s.name, s.price, s.rarity, s.image_url, g.name AS game_name " +
            "FROM skin s LEFT JOIN game g ON s.game_id = g.id WHERE 1=1"
        );
        if (hasGameFilter) sql.append(" AND g.name LIKE ?");
        if (hasSearch)     sql.append(" AND s.name LIKE ?");
        
        if (sortCombo != null && sortCombo.getValue() != null) {
            String sortOption = sortCombo.getValue();
            if ("Prix: Croissant".equals(sortOption)) sql.append(" ORDER BY s.price ASC");
            else if ("Nom: A-Z".equals(sortOption)) sql.append(" ORDER BY s.name ASC");
            else if ("Nom: Z-A".equals(sortOption)) sql.append(" ORDER BY s.name DESC");
            else sql.append(" ORDER BY s.price DESC");
        } else {
            sql.append(" ORDER BY s.price DESC");
        }
        sql.append(" LIMIT 24");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int i = 1;
            if (hasGameFilter) ps.setString(i++, "%" + selectedGame + "%");
            if (hasSearch)     ps.setString(i++, "%" + search + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                java.util.UUID skinId = com.carthage.utils.UUIDUtils.fromBytes(rs.getBytes("id"));
                skinsGrid.getChildren().add(buildSkinCard(
                    skinId,
                    rs.getString("name"),
                    rs.getInt("price"),
                    rs.getString("rarity"),
                    rs.getString("game_name"),
                    rs.getString("image_url")
                ));
            }
            if (skinsGrid.getChildren().isEmpty()) {
                Label empty = new Label("Aucun skin trouvé.");
                empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20 0;");
                skinsGrid.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Erreur DB: " + e.getMessage());
            err.setStyle("-fx-text-fill: #FF4D4D; -fx-font-size: 13px;");
            skinsGrid.getChildren().add(err);
        }
    }

    private VBox buildSkinCard(java.util.UUID id, String name, int price, String rarity, String game, String imageUrl) {
        VBox card = new VBox(0);
        card.setPrefWidth(205);
        card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E2633; -fx-background-radius: 12px; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;"));
        
        card.setOnMouseClicked(e -> {
            MainLayoutController mlc = (MainLayoutController) card.getScene().lookup("#contentArea").getUserData();
            mlc.loadSkinDetail(id);
        });

        // ── Image area ──
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(155);
        imgPane.setStyle("-fx-background-color: #1E2633; -fx-background-radius: 12px 12px 0 0;");
        
        Label placeholder = new Label("🎮");
        placeholder.setStyle("-fx-font-size: 38px;");
        
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                ImageView iv = new ImageView(new Image(imageUrl, true));
                iv.setFitWidth(205);
                iv.setFitHeight(155);
                iv.setPreserveRatio(true);
                imgPane.getChildren().add(iv);
            } catch (Exception e) {
                imgPane.getChildren().add(placeholder);
            }
        } else {
            imgPane.getChildren().add(placeholder);
        }

        Label gameBadge = new Label(game != null ? game.toUpperCase() : "SKIN");
        gameBadge.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-text-fill: #9CA3AF;" +
            " -fx-font-size: 9px; -fx-background-radius: 4px; -fx-padding: 2 6;");
        StackPane.setAlignment(gameBadge, Pos.TOP_LEFT);
        StackPane.setMargin(gameBadge, new Insets(8));
        imgPane.getChildren().add(gameBadge);

        // ── Body ──
        VBox body = new VBox(6);
        body.setPadding(new Insets(10, 12, 12, 12));

        Label nameLabel = new Label(name != null ? name : "—");
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        // Rarity badge
        String rarityUpper = rarity != null ? rarity.toUpperCase() : "COMMON";
        String rarityColor = switch (rarityUpper) {
            case "RARE"      -> "#3B82F6";
            case "EPIC"      -> "#8B5CF6";
            case "LEGENDARY" -> "#F59E0B";
            default          -> "#6b7280";
        };
        Label rarityBadge = new Label(rarityUpper);
        rarityBadge.setStyle("-fx-background-color: " + rarityColor + "33; -fx-text-fill: " + rarityColor +
            "; -fx-background-radius: 4px; -fx-padding: 1 7; -fx-font-size: 10px; -fx-font-weight: bold;");

        // Price row
        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label("💰 " + price);
        priceLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label insuffLabel = new Label("PC INSUFFISANTS");
        insuffLabel.setStyle("-fx-text-fill: -carthage-accent; -fx-font-size: 9px;");
        priceRow.getChildren().addAll(priceLabel, insuffLabel);

        // Buy button
        Button buyBtn = new Button();
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        com.carthage.entity.User user = com.carthage.utils.SessionContext.getInstance().getCurrentUser();
        
        if (user == null) {
            buyBtn.setText("Non Connecté");
            buyBtn.setStyle("-fx-background-color: #0B0E14; -fx-text-fill: #6b7280; -fx-background-radius: 8px; -fx-padding: 7 0; -fx-cursor: not-allowed;");
            insuffLabel.setVisible(false);
            insuffLabel.setManaged(false);
        } else if (user.getBalance() >= price) {
            buyBtn.setText("Acheter");
            buyBtn.setStyle("-fx-background-color: -carthage-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 7 0; -fx-cursor: hand;");
            buyBtn.setOnAction(evt -> handlePurchase(id, name, price, user));
            insuffLabel.setVisible(false);
            insuffLabel.setManaged(false);
        } else {
            buyBtn.setText("Points Insuffisants");
            buyBtn.setStyle("-fx-background-color: #0B0E14; -fx-text-fill: #6b7280; -fx-background-radius: 8px; -fx-padding: 7 0; -fx-cursor: not-allowed;");
            insuffLabel.setVisible(true);
            insuffLabel.setManaged(true);
        }

        body.getChildren().addAll(nameLabel, rarityBadge, priceRow, buyBtn);
        card.getChildren().addAll(imgPane, body);
        return card;
    }

    private void handlePurchase(java.util.UUID skinId, String skinName, int price, com.carthage.entity.User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous acheter " + skinName + " pour " + price + " PC ?");
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                user.setBalance(user.getBalance() - price);
                Alert success = new Alert(Alert.AlertType.INFORMATION, "Achat réussi !");
                success.showAndWait();
                loadSkins();
            }
        });
    }
}
