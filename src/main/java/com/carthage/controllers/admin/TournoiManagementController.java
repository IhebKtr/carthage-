package com.carthage.controllers.admin;

import com.carthage.entity.Tournoi;
import com.carthage.services.TournoiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TournoiManagementController {

    @FXML private TableView<Tournoi> tournoiTable;
    @FXML private TableColumn<Tournoi, String> colId;
    @FXML private TableColumn<Tournoi, String> colNom;
    @FXML private TableColumn<Tournoi, String> colJeu;
    @FXML private TableColumn<Tournoi, Tournoi> colEquipes;
    @FXML private TableColumn<Tournoi, String> colStatut;
    @FXML private TableColumn<Tournoi, String> colCagnotte;
    @FXML private TableColumn<Tournoi, String> colDate;
    @FXML private TableColumn<Tournoi, Tournoi> colActions;

    private final TournoiService service = new TournoiService();

    @FXML
    public void initialize() {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(cell -> {
            String fullId = cell.getValue().getId().toString();
            return new SimpleStringProperty("#" + fullId.substring(0, 8));
        });

        colNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));

        // Blue Pill for Game
        colJeu.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getGame().getName()));
        colJeu.setCellFactory(param -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #3B82F6; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.setText(item);
                    setGraphic(badge);
                }
            }
        });

        // Equipes column (Text + Progress Line)
        colEquipes.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colEquipes.setCellFactory(param -> new TableCell<>() {
            private final VBox box = new VBox(2);
            private final Label textLabel = new Label();
            private final Line bgLine = new Line(0,0, 50,0);
            private final Line fillLine = new Line(0,0, 0,0);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                textLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
                bgLine.setStyle("-fx-stroke: #2A3441; -fx-stroke-width: 3; -fx-stroke-line-cap: round;");
                fillLine.setStyle("-fx-stroke: -carthage-accent; -fx-stroke-width: 3; -fx-stroke-line-cap: round;");
                
                // Stack lines
                javafx.scene.layout.StackPane progressStack = new javafx.scene.layout.StackPane(bgLine, fillLine);
                progressStack.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(textLabel, progressStack);
            }

            @Override
            protected void updateItem(Tournoi item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    int current = item.getTeams() != null ? item.getTeams().size() : 0;
                    int max = item.getNbEquipesMax();
                    textLabel.setText(current + " / " + max);
                    
                    double percentage = max == 0 ? 0 : (double) current / max;
                    fillLine.setEndX(50 * percentage);
                    
                    setGraphic(box);
                }
            }
        });

        colStatut.setCellValueFactory(cell -> {
            String status = cell.getValue().getStatus() != null ? cell.getValue().getStatus().name() : "UNKNOWN";
            return new SimpleStringProperty(status);
        });
        colStatut.setCellFactory(param -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("status-badge");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.setText("• " + item);
                    badge.getStyleClass().removeAll("status-actif", "status-inactif");
                    if (item.equalsIgnoreCase("ACTIF")) {
                        badge.getStyleClass().add("status-actif");
                    } else {
                        badge.setStyle("-fx-background-color: rgba(251, 191, 36, 0.2); -fx-text-fill: #FBBF24; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                }
            }
        });

        colCagnotte.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPrizePool() + " DT"));
        colCagnotte.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold;");
                }
            }
        });

        colDate.setCellValueFactory(cell -> {
            if (cell.getValue().getDateDebut() != null) {
                return new SimpleStringProperty(cell.getValue().getDateDebut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            return new SimpleStringProperty("");
        });

        colActions.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("👁");
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final HBox actions = new HBox(5, viewBtn, editBtn, deleteBtn);
            {
                viewBtn.getStyleClass().add("btn-icon-action");
                editBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.getStyleClass().add("btn-icon-action");
                deleteBtn.setStyle("-fx-text-fill: #FF4D4D;");
                
                editBtn.setOnAction(e -> handleEdit(getItem()));
                deleteBtn.setOnAction(e -> handleDelete(getItem()));
                viewBtn.setOnAction(e -> handleEdit(getItem()));
            }
            @Override
            protected void updateItem(Tournoi item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(actions);
                }
            }
        });
    }

    private void loadData() {
        List<Tournoi> data = service.getAllTournois();
        tournoiTable.setItems(FXCollections.observableArrayList(data));
    }

    @FXML
    public void handleCreate() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/carthage/view/admin/tournoi-create-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            TournoiCreateController controller = loader.getController();
            controller.setOnSuccessCallback(this::loadData);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Tournoi t) {
        if (t == null) return;
        try {
            AdminMainLayoutController main = (AdminMainLayoutController) tournoiTable.getScene().lookup("#contentArea").getUserData();
            if (main != null) {
                TournoiEditController editController = main.loadViewAndGetController("/com/carthage/view/admin/tournoi-edit-view.fxml");
                if (editController != null) {
                    editController.setTournamentAndMainController(t.getId(), main);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Tournoi t) {
        if (t == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete tournament " + t.getNom() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(t.getId());
                    loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
