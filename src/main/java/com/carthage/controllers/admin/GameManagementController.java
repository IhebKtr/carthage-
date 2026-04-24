package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.services.GameService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

public class GameManagementController {

    @FXML
    private FlowPane gamesGrid;

    @FXML
    private TextField searchField;

    private final GameService gameService = new GameService();
    private List<Game> allGames;

    @FXML
    public void initialize() {
        loadGames();
    }

    private void loadGames() {
        allGames = gameService.getAllGames();
        renderGames(allGames);
    }

    private void renderGames(List<Game> games) {
        gamesGrid.getChildren().clear();
        for (Game game : games) {
            gamesGrid.getChildren().add(createGameCard(game));
        }
    }

    private VBox createGameCard(Game game) {
        VBox card = new VBox();
        card.getStyleClass().add("game-card");
        card.setPrefWidth(300);
        card.setSpacing(10);

        // Banner Stack
        StackPane bannerStack = new StackPane();
        bannerStack.setPrefHeight(150);
        bannerStack.getStyleClass().add("game-card-banner-container");

        ImageView banner = new ImageView();
        try {
            if (game.getImageUrl() != null && !game.getImageUrl().isEmpty()) {
                banner.setImage(new Image(game.getImageUrl()));
            }
        } catch (Exception e) {
            // fallback
        }
        banner.setFitWidth(300);
        banner.setFitHeight(150);
        // We'll mask it using CSS later if needed

        // Status badge
        Label statusBadge = new Label(game.getStatus().toString());
        statusBadge.getStyleClass().add("status-badge");
        if (game.getStatus().toString().equalsIgnoreCase("ACTIF")) {
            statusBadge.getStyleClass().add("status-actif");
        } else {
            statusBadge.getStyleClass().add("status-inactif");
        }
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new javafx.geometry.Insets(10));

        bannerStack.getChildren().addAll(banner, statusBadge);

        // Content Area
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(15));
        content.getStyleClass().add("game-card-content");

        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(game.getName());
        title.getStyleClass().add("game-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label typeLabel = new Label(game.getType().toString());
        typeLabel.getStyleClass().add("game-card-type");
        titleBox.getChildren().addAll(title, spacer, typeLabel);

        Label subLabel = new Label("Publisher - " + game.getName());
        subLabel.getStyleClass().add("game-card-subtitle");

        // Stats Box
        HBox statsBox = new HBox(10);
        statsBox.setAlignment(Pos.CENTER);

        VBox tournoiStat = new VBox(5);
        tournoiStat.setAlignment(Pos.CENTER);
        tournoiStat.getStyleClass().add("stat-box-mini");
        Label tLabel = new Label("TOURNOIS");
        tLabel.getStyleClass().add("stat-label-mini");
        Label tValue = new Label(String.valueOf(game.getTournois() != null ? game.getTournois().size() : 0));
        tValue.getStyleClass().add("stat-value-yellow");
        tournoiStat.getChildren().addAll(tLabel, tValue);

        VBox skinStat = new VBox(5);
        skinStat.setAlignment(Pos.CENTER);
        skinStat.getStyleClass().add("stat-box-mini");
        Label sLabel = new Label("SKINS");
        sLabel.getStyleClass().add("stat-label-mini");
        Label sValue = new Label(String.valueOf(game.getSkins() != null ? game.getSkins().size() : 0));
        sValue.getStyleClass().add("stat-value-green");
        skinStat.getChildren().addAll(sLabel, sValue);

        HBox.setHgrow(tournoiStat, Priority.ALWAYS);
        HBox.setHgrow(skinStat, Priority.ALWAYS);
        tournoiStat.setMaxWidth(Double.MAX_VALUE);
        skinStat.setMaxWidth(Double.MAX_VALUE);
        statsBox.getChildren().addAll(tournoiStat, skinStat);

        // Actions
        HBox actionsBox = new HBox(10);
        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().addAll("btn-outline");
        Button btnDelete = new Button("Supprimer");
        btnDelete.getStyleClass().addAll("btn-outline", "btn-delete");

        HBox.setHgrow(btnEdit, Priority.ALWAYS);
        HBox.setHgrow(btnDelete, Priority.ALWAYS);
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setMaxWidth(Double.MAX_VALUE);

        actionsBox.getChildren().addAll(btnEdit, btnDelete);

        content.getChildren().addAll(titleBox, subLabel, statsBox, actionsBox);
        card.getChildren().addAll(bannerStack, content);

        return card;
    }
}
