package com.carthage.controllers.admin;

import com.carthage.entity.Tournoi;
import com.carthage.services.TournoiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TournoiManagementController {

    @FXML private VBox tournoisListContainer;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> statusFilter;

    private final TournoiService service = new TournoiService();
    private ObservableList<Tournoi> masterData;
    private FilteredList<Tournoi> filteredData;

    @FXML
    public void initialize() {
        loadData();
        setupFilters();
        renderList();
    }

    // ─── Search & Filter Logic ───────────────────────────────────────

    private void setupFilters() {
        // Populate game filter from loaded data
        List<String> gameNames = masterData.stream()
                .map(t -> t.getGame() != null ? t.getGame().getName() : "Inconnu")
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        gameNames.add(0, "Tous les jeux");
        gameFilter.setItems(FXCollections.observableArrayList(gameNames));
        gameFilter.setValue("Tous les jeux");

        // Populate status filter
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "UPCOMING", "ONGOING", "COMPLETED"
        ));
        statusFilter.setValue("Tous les statuts");

        // Attach listeners — each change re-evaluates the combined predicate
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        gameFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(tournoi -> {
            // 1. Search text filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isBlank()) {
                String lower = searchText.toLowerCase();
                boolean matchName = tournoi.getNom() != null && tournoi.getNom().toLowerCase().contains(lower);
                boolean matchGame = tournoi.getGame() != null && tournoi.getGame().getName() != null
                        && tournoi.getGame().getName().toLowerCase().contains(lower);
                if (!matchName && !matchGame) return false;
            }

            // 2. Game filter
            String selectedGame = gameFilter.getValue();
            if (selectedGame != null && !"Tous les jeux".equals(selectedGame)) {
                String gameName = tournoi.getGame() != null ? tournoi.getGame().getName() : "";
                if (!selectedGame.equals(gameName)) return false;
            }

            // 3. Status filter
            String selectedStatus = statusFilter.getValue();
            if (selectedStatus != null && !"Tous les statuts".equals(selectedStatus)) {
                String status = tournoi.getStatus() != null ? tournoi.getStatus().name() : "";
                if (!selectedStatus.equalsIgnoreCase(status)) return false;
            }

            return true;
        });
        
        // After updating the predicate, re-render the list
        renderList();
    }

    // ─── List Rendering Logic ────────────────────────────────────────

    private void renderList() {
        tournoisListContainer.getChildren().clear();
        
        if (filteredData.isEmpty()) {
            Label emptyLabel = new Label("Aucun tournoi trouvé.");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-padding: 30;");
            tournoisListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Tournoi t : filteredData) {
            tournoisListContainer.getChildren().add(createRow(t));
        }
    }
    
    private HBox createRow(Tournoi t) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 15 20; -fx-border-color: #2A3441; -fx-border-width: 0 0 1 0;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 15 20; -fx-border-color: #2A3441; -fx-border-width: 0 0 1 0; -fx-background-color: rgba(255,255,255,0.02);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding: 15 20; -fx-border-color: #2A3441; -fx-border-width: 0 0 1 0;"));

        // 1. ID
        String fullId = t.getId().toString();
        Label idLabel = new Label("#" + fullId.substring(0, 8));
        idLabel.setPrefWidth(80);
        idLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-family: monospace;");
        
        // 2. NOM
        Label nomLabel = new Label(t.getNom());
        nomLabel.setPrefWidth(180);
        nomLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        nomLabel.setWrapText(true);
        
        // 3. JEU (badge)
        Label gameBadge = new Label(t.getGame() != null ? t.getGame().getName() : "Inconnu");
        gameBadge.setStyle("-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #3B82F6; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
        HBox gameBox = new HBox(gameBadge);
        gameBox.setPrefWidth(120);
        gameBox.setAlignment(Pos.CENTER_LEFT);
        
        // 4. ÉQUIPES (Progress bar)
        VBox teamsBox = new VBox(2);
        teamsBox.setPrefWidth(120);
        teamsBox.setAlignment(Pos.CENTER_LEFT);
        Label teamsText = new Label();
        teamsText.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Line bgLine = new Line(0,0, 50,0);
        bgLine.setStyle("-fx-stroke: #2A3441; -fx-stroke-width: 3; -fx-stroke-line-cap: round;");
        Line fillLine = new Line(0,0, 0,0);
        fillLine.setStyle("-fx-stroke: -carthage-accent; -fx-stroke-width: 3; -fx-stroke-line-cap: round;");
        
        StackPane progressStack = new StackPane(bgLine, fillLine);
        progressStack.setAlignment(Pos.CENTER_LEFT);
        teamsBox.getChildren().addAll(teamsText, progressStack);
        
        int current = t.getTeams() != null ? t.getTeams().size() : 0;
        int max = t.getNbEquipesMax();
        teamsText.setText(current + " / " + max);
        double percentage = max == 0 ? 0 : Math.min(1.0, (double) current / max);
        fillLine.setEndX(50 * percentage);
        
        // 5. STATUT
        String statusName = t.getStatus() != null ? t.getStatus().name() : "UNKNOWN";
        Label statusBadge = new Label("• " + statusName);
        HBox statusBox = new HBox(statusBadge);
        statusBox.setPrefWidth(100);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        
        String color;
        String bgColor;
        switch (statusName.toUpperCase()) {
            case "UPCOMING" -> { color = "#22C55E"; bgColor = "rgba(34, 197, 94, 0.2)"; }
            case "ONGOING"  -> { color = "#F59E0B"; bgColor = "rgba(245, 158, 11, 0.2)"; }
            case "COMPLETED" -> { color = "#6B7280"; bgColor = "rgba(107, 114, 128, 0.2)"; }
            default -> { color = "#FBBF24"; bgColor = "rgba(251, 191, 36, 0.2)"; }
        }
        statusBadge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + color +
                "; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");

        // 6. CAGNOTTE
        Label cagnotteLabel = new Label(t.getPrizePool() + " DT");
        cagnotteLabel.setPrefWidth(100);
        cagnotteLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold;");

        // 7. DATE
        String dateStr = t.getDateDebut() != null ? t.getDateDebut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.setPrefWidth(120);
        dateLabel.setStyle("-fx-text-fill: white;");

        // 8. ACTIONS
        Button viewBtn = new Button("👁");
        Button editBtn = new Button("✎");
        Button deleteBtn = new Button("🗑");
        
        viewBtn.getStyleClass().add("btn-icon-action");
        editBtn.getStyleClass().add("btn-icon-action");
        deleteBtn.getStyleClass().add("btn-icon-action");
        deleteBtn.setStyle("-fx-text-fill: #FF4D4D;");
        
        viewBtn.setOnAction(e -> handleEdit(t));
        editBtn.setOnAction(e -> handleEdit(t));
        deleteBtn.setOnAction(e -> handleDelete(t));

        HBox actionsBox = new HBox(5, viewBtn, editBtn, deleteBtn);
        actionsBox.setPrefWidth(120);
        actionsBox.setAlignment(Pos.CENTER);
        
        row.getChildren().addAll(idLabel, nomLabel, gameBox, teamsBox, statusBox, cagnotteLabel, dateLabel, actionsBox);
        return row;
    }

    // ─── Data Loading ────────────────────────────────────────────────

    private void loadData() {
        List<Tournoi> data = service.getAllTournois();
        masterData = FXCollections.observableArrayList(data);
        filteredData = new FilteredList<>(masterData, p -> true);
    }

    // ─── CRUD Actions ────────────────────────────────────────────────

    @FXML
    public void handleCreate() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/carthage/view/admin/tournoi-create-dialog.fxml"));
            javafx.scene.Parent root = loader.load();
            
            TournoiCreateController controller = loader.getController();
            controller.setOnSuccessCallback(this::refreshData);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            // ESC closes the dialog (cancel without saving), with a discard-changes
            // confirmation if the form is dirty. The undecorated stage has no
            // window chrome so ESC is the primary keyboard escape.
            scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    if (controller.confirmCloseIfDirty()) {
                        stage.close();
                    }
                    ev.consume();
                }
            });
            // Same guard if the OS sends a close request (e.g. Alt+F4).
            stage.setOnCloseRequest(ev -> {
                if (!controller.confirmCloseIfDirty()) {
                    ev.consume();
                }
            });
            stage.setScene(scene);
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /** Navigate to the Anomaly Detection view. */
    @FXML
    public void handleShowAnomalies() {
        try {
            AdminMainLayoutController main = (AdminMainLayoutController) searchField.getScene().lookup("#contentArea").getUserData();
            if (main != null) {
                main.loadView("/com/carthage/view/admin/tournoi-anomaly-view.fxml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Tournoi t) {
        if (t == null) return;
        try {
            // Find MainLayoutController from the root node scene (using searchField as reference)
            AdminMainLayoutController main = (AdminMainLayoutController) searchField.getScene().lookup("#contentArea").getUserData();
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le tournoi " + t.getNom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(t.getId());
                    refreshData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /** Reload data from DB and refresh filters (keeps current filter/search state). */
    private void refreshData() {
        List<Tournoi> data = service.getAllTournois();
        masterData.setAll(data);

        // Refresh game filter options
        List<String> gameNames = masterData.stream()
                .map(t -> t.getGame() != null ? t.getGame().getName() : "Inconnu")
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        gameNames.add(0, "Tous les jeux");
        String currentGame = gameFilter.getValue();
        gameFilter.setItems(FXCollections.observableArrayList(gameNames));
        gameFilter.setValue(gameNames.contains(currentGame) ? currentGame : "Tous les jeux");

        applyFilters(); 
        // Note: applyFilters will automatically call renderList()
    }
}
