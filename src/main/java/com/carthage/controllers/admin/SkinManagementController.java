package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.Skin;
import com.carthage.entity.enums.SkinRarity;
import com.carthage.services.GameService;
import com.carthage.services.SkinService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SkinManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> gameFilterCombo;
    @FXML private ComboBox<String> rarityFilterCombo;

    @FXML private TableView<Skin> skinTable;
    @FXML private TableColumn<Skin, String> colImage;
    @FXML private TableColumn<Skin, String> colName;
    @FXML private TableColumn<Skin, String> colGame;
    @FXML private TableColumn<Skin, Integer> colPrice;
    @FXML private TableColumn<Skin, String> colRarity;
    @FXML private TableColumn<Skin, Void> colActions;

    private final SkinService skinService = new SkinService();
    private final GameService gameService = new GameService();
    private ObservableList<Skin> allSkins = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        colGame.setCellValueFactory(cellData -> {
            Game g = cellData.getValue().getGame();
            return new SimpleStringProperty(g != null ? g.getName() : "—");
        });
        colPrice.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getPrice()).asObject());
        colRarity.setCellValueFactory(cellData -> {
            SkinRarity r = cellData.getValue().getRarity();
            return new SimpleStringProperty(r != null ? r.name() : "—");
        });

        colImage.setCellFactory(param -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Skin skin = getTableRow().getItem();
                    if (skin.getImageUrl() != null && !skin.getImageUrl().isBlank()) {
                        try {
                            imageView.setImage(new Image(skin.getImageUrl(), true));
                            setGraphic(imageView);
                        } catch (Exception e) {
                            setGraphic(new Label("🎮"));
                        }
                    } else {
                        setGraphic(new Label("🎮"));
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.getStyleClass().addAll("btn-icon-action", "btn-delete");

                editBtn.setOnAction(event -> {
                    Skin skin = getTableView().getItems().get(getIndex());
                    openDialog(skin);
                });

                deleteBtn.setOnAction(event -> {
                    Skin skin = getTableView().getItems().get(getIndex());
                    handleDelete(skin);
                });
                
                pane.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupFilters() {
        gameFilterCombo.getItems().add("Tous les Jeux");
        for (Game g : gameService.getAllGames()) {
            gameFilterCombo.getItems().add(g.getName());
        }
        gameFilterCombo.setValue("Tous les Jeux");

        rarityFilterCombo.getItems().add("Toutes Raretés");
        for (SkinRarity r : SkinRarity.values()) {
            rarityFilterCombo.getItems().add(r.name());
        }
        rarityFilterCombo.setValue("Toutes Raretés");

        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        gameFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
        rarityFilterCombo.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void loadData() {
        allSkins.setAll(skinService.getAllSkins());
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String game = gameFilterCombo.getValue();
        String rarity = rarityFilterCombo.getValue();

        List<Skin> filtered = allSkins.stream()
            .filter(s -> search.isEmpty() || (s.getName() != null && s.getName().toLowerCase().contains(search)))
            .filter(s -> "Tous les Jeux".equals(game) || (s.getGame() != null && game.equals(s.getGame().getName())))
            .filter(s -> "Toutes Raretés".equals(rarity) || (s.getRarity() != null && rarity.equals(s.getRarity().name())))
            .collect(Collectors.toList());

        skinTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onAddSkin() {
        openDialog(null);
    }

    private void openDialog(Skin skin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/admin/skin-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            
            SkinDialogController controller = loader.getController();
            controller.setSkinData(skin);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
            
            controller.setOnSaveCallback(() -> {
                loadData();
                stage.close();
            });
            controller.setOnCancelCallback(stage::close);
            
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Skin skin) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Êtes-vous sûr de vouloir supprimer le skin " + skin.getName() + " ?");
        alert.setHeaderText("Confirmation de suppression");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                boolean success = skinService.delete(skin.getId());
                if (success) {
                    loadData();
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Impossible de supprimer ce skin. Il est peut-être lié à d'autres données (ex: commandes, inventaires).");
                    errorAlert.setHeaderText("Erreur de suppression");
                    errorAlert.showAndWait();
                }
            }
        });
    }
}
