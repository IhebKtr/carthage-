package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.services.GameService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.ComboBox;

import java.util.List;

public class GameManagementController {

    @FXML
    private FlowPane gamesGrid;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeFilter;

    @FXML
    private ComboBox<String> statusFilter;

    private final GameService gameService = new GameService();
    private List<Game> allGames;

    @FXML
    public void initialize() {
        loadGames();
        setupSearchAndFilters();
    }

    private void setupSearchAndFilters() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterGames());
        
        typeFilter.getItems().add("Toutes les catégories");
        for (com.carthage.entity.enums.GameType type : com.carthage.entity.enums.GameType.values()) {
            typeFilter.getItems().add(type.name());
        }
        typeFilter.setValue("Toutes les catégories");
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterGames());

        statusFilter.getItems().add("Tous les statuts");
        for (com.carthage.entity.enums.GameStatus status : com.carthage.entity.enums.GameStatus.values()) {
            statusFilter.getItems().add(status.name());
        }
        statusFilter.setValue("Tous les statuts");
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterGames());
    }

    private void filterGames() {
        if (allGames == null) return;
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedType = typeFilter.getValue();
        String selectedStatus = statusFilter.getValue();
        
        List<Game> filtered = allGames.stream()
                .filter(game -> game.getName() != null && game.getName().toLowerCase().contains(searchText))
                .filter(game -> "Toutes les catégories".equals(selectedType) || (game.getType() != null && game.getType().name().equals(selectedType)))
                .filter(game -> "Tous les statuts".equals(selectedStatus) || (game.getStatus() != null && game.getStatus().name().equals(selectedStatus)))
                .toList();
        renderGames(filtered);
    }

    @FXML
    public void handleAdd() {
        openGameDialog(null);
    }

    private void handleEdit(Game game) {
        openGameDialog(game);
    }

    private void openGameDialog(Game game) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/carthage/view/admin/game-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            GameDialogController controller = loader.getController();
            controller.setOnSuccessCallback(this::loadGames);
            if (game != null) {
                controller.setGameForEdit(game);
            }
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Game game) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer le jeu " + game.getName() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    gameService.delete(game.getId());
                    loadGames();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
                // The 'true' at the end enables background loading
                Image img = new Image(game.getImageUrl(), true);
                banner.setImage(img);
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
        if (game.getStatus().toString().equalsIgnoreCase("ACTIVE")) {
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

        Label subLabel = new Label(game.getDescription() != null && !game.getDescription().isEmpty() ? game.getDescription() : "Aucune description disponible");
        subLabel.getStyleClass().add("game-card-subtitle");
        subLabel.setWrapText(true);
        subLabel.setMaxHeight(40);
        subLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);

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
        btnEdit.setOnAction(e -> handleEdit(game));
        
        Button btnDelete = new Button("Supprimer");
        btnDelete.getStyleClass().addAll("btn-outline", "btn-delete");
        btnDelete.setOnAction(e -> handleDelete(game));
        
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
