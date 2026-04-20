package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.Team;
import com.carthage.entity.Tournoi;
import com.carthage.entity.User;
import com.carthage.entity.enums.TournamentStatus;
import com.carthage.entity.enums.TournamentType;
import com.carthage.services.TournoiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.gluonhq.maps.MapLayer;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TournoiEditController {

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<Game> gameCombo;
    @FXML
    private ComboBox<TournamentType> typeCombo;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextField maxTeamsField;
    @FXML
    private TextField prizeField;
    @FXML
    private ComboBox<TournamentStatus> statusCombo;
    @FXML
    private ComboBox<User> refereeCombo;
    @FXML
    private ListView<Team> teamsList;
    @FXML
    private ComboBox<Team> winnerCombo;
    @FXML
    private ComboBox<String> placeCombo;
    @FXML
    private MapView mapWebView;
    
    private MapLayer markerLayer;

    @FXML
    private Label infoStartDate;
    @FXML
    private Label infoParticipants;
    @FXML
    private Label smartWarningLabel;

    private final TournoiService service = new TournoiService();
    private Tournoi tournoi;
    private AdminMainLayoutController mainController;

    @FXML
    public void initialize() {
        // Init Combos
        List<Game> games = service.getAllGames();
        gameCombo.setItems(FXCollections.observableArrayList(games));
        gameCombo.setConverter(createGameConverter());

        typeCombo.setItems(FXCollections.observableArrayList(TournamentType.values()));
        statusCombo.setItems(FXCollections.observableArrayList(TournamentStatus.values()));

        List<User> refs = service.getReferees();
        refereeCombo.setItems(FXCollections.observableArrayList(refs));
        refereeCombo.setConverter(createUserConverter());

        teamsList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    Label lbl = new Label("🎮 " + (item.getName() != null ? item.getName() : "Unknown Team"));
                    lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
                    setGraphic(lbl);
                    setText(null);
                    setStyle(
                            "-fx-background-color: #1A212D; -fx-background-radius: 6; -fx-padding: 8; -fx-border-color: #2A3441; -fx-border-radius: 6; -fx-border-width: 1; -fx-margin: 2;");
                }
            }
        });

        // Place list
        placeCombo.setItems(
                FXCollections.observableArrayList("Tunis", "Sousse", "Sfax", "Monastir", "Bizerte", "Esprit, Ghazela"));
        placeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateMap(newVal));
    }

    private class CustomMapLayer extends MapLayer {
        private final Circle circle;
        private final MapPoint mapPoint;

        public CustomMapLayer(MapPoint mapPoint) {
            this.mapPoint = mapPoint;
            this.circle = new Circle(8, Color.web("#E50914"));
            this.circle.setStroke(Color.WHITE);
            this.circle.setStrokeWidth(2);
            this.getChildren().add(circle);
        }

        @Override
        protected void layoutLayer() {
            Point2D point = getMapPoint(mapPoint.getLatitude(), mapPoint.getLongitude());
            if (point != null) {
                circle.setTranslateX(point.getX());
                circle.setTranslateY(point.getY());
            }
        }
    }

    private void updateMap(String place) {
        if (place == null || mapWebView == null)
            return;

        double lat;
        double lon;
        switch (place) {
            case "Sousse":        lat = 35.8254; lon = 10.6369; break;
            case "Sfax":         lat = 34.7406; lon = 10.7603; break;
            case "Monastir":     lat = 35.7780; lon = 10.8262; break;
            case "Bizerte":      lat = 37.2744; lon = 9.8739;  break;
            case "Esprit, Ghazela": lat = 36.8985; lon = 10.1898; break;
            default:             lat = 36.8065; lon = 10.1815; break;
        }

        MapPoint mapPoint = new MapPoint(lat, lon);
        mapWebView.setCenter(mapPoint);
        mapWebView.setZoom(13);
        
        if (markerLayer != null) {
            mapWebView.removeLayer(markerLayer);
        }
        
        markerLayer = new CustomMapLayer(mapPoint);
        mapWebView.addLayer(markerLayer);
    }

    public void setTournamentAndMainController(UUID id, AdminMainLayoutController mainController) {
        this.mainController = mainController;
        this.tournoi = service.getById(id);
        if (tournoi != null) {
            populateFields();
        }
    }

    private void populateFields() {
        nameField.setText(tournoi.getNom());
        if (tournoi.getDateDebut() != null)
            startDatePicker.setValue(tournoi.getDateDebut().toLocalDate());
        if (tournoi.getDateFin() != null)
            endDatePicker.setValue(tournoi.getDateFin().toLocalDate());
        maxTeamsField.setText(String.valueOf(tournoi.getNbEquipesMax()));
        prizeField.setText(String.valueOf(tournoi.getPrizePool()));

        if (tournoi.getPlace() != null && placeCombo.getItems().contains(tournoi.getPlace())) {
            placeCombo.setValue(tournoi.getPlace());
        } else {
            placeCombo.setValue("Tunis");
        }

        typeCombo.setValue(tournoi.getType());
        statusCombo.setValue(tournoi.getStatus());

        // Preselect Game
        if (tournoi.getGame() != null && tournoi.getGame().getId() != null) {
            gameCombo.getItems().stream()
                    .filter(g -> g.getId().equals(tournoi.getGame().getId()))
                    .findFirst().ifPresent(gameCombo::setValue);
        }

        // Preselect Referee
        if (tournoi.getReferee() != null && tournoi.getReferee().getId() != null) {
            refereeCombo.getItems().stream()
                    .filter(u -> u.getId().equals(tournoi.getReferee().getId()))
                    .findFirst().ifPresent(refereeCombo::setValue);
        }

        // Load Teams
        List<Team> participatingTeams = service.getTeamsForTournament(tournoi.getId());
        teamsList.setItems(FXCollections.observableArrayList(participatingTeams));

        // Add null option so winner can be cleared, then load teams
        List<Team> winnerOptions = new java.util.ArrayList<>();
        winnerOptions.add(null);
        winnerOptions.addAll(participatingTeams);
        winnerCombo.setItems(FXCollections.observableArrayList(winnerOptions));
        winnerCombo.setConverter(createTeamConverter());
        if (tournoi.getWinner() != null && tournoi.getWinner().getId() != null) {
            winnerCombo.getItems().stream()
                    .filter(t -> t.getId().equals(tournoi.getWinner().getId()))
                    .findFirst().ifPresent(winnerCombo::setValue);
        }

        // Side Pane Info
        if (tournoi.getDateDebut() != null) {
            infoStartDate.setText(tournoi.getDateDebut().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        }
        infoParticipants.setText(participatingTeams.size() + " / " + tournoi.getNbEquipesMax());

        // Always refresh map after populating (listener may not fire if value is
        // unchanged)
        updateMap(placeCombo.getValue());
    }

    @FXML
    public void handleUpdate() {
        if (tournoi == null || !validateInput())
            return;

        tournoi.setNom(nameField.getText());
        if (startDatePicker.getValue() != null)
            tournoi.setDateDebut(startDatePicker.getValue().atStartOfDay());
        if (endDatePicker.getValue() != null)
            tournoi.setDateFin(endDatePicker.getValue().atStartOfDay());

        tournoi.setNbEquipesMax(Integer.parseInt(maxTeamsField.getText()));
        tournoi.setPrizePool(Integer.parseInt(prizeField.getText()));

        tournoi.setPlace(placeCombo.getValue());

        tournoi.setType(typeCombo.getValue());
        tournoi.setStatus(statusCombo.getValue());
        tournoi.setGame(gameCombo.getValue());
        tournoi.setReferee(refereeCombo.getValue());
        tournoi.setWinner(winnerCombo.getValue());

        try {
            service.update(tournoi);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Tournoi mis à jour !");
            alert.showAndWait();
            // Update the side pane info
            populateFields();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Reset styles
        nameField.getStyleClass().remove("input-error");
        startDatePicker.getStyleClass().remove("input-error");
        endDatePicker.getStyleClass().remove("input-error");
        maxTeamsField.getStyleClass().remove("input-error");
        prizeField.getStyleClass().remove("input-error");
        gameCombo.getStyleClass().remove("input-error");
        statusCombo.getStyleClass().remove("input-error");
        typeCombo.getStyleClass().remove("input-error");
        refereeCombo.getStyleClass().remove("input-error");
        placeCombo.getStyleClass().remove("input-error");

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errors.append("- Le nom du tournoi est obligatoire.\n");
            nameField.getStyleClass().add("input-error");
        }

        if (startDatePicker.getValue() == null) {
            errors.append("- La date de début est obligatoire.\n");
            startDatePicker.getStyleClass().add("input-error");
        }
        if (endDatePicker.getValue() == null) {
            errors.append("- La date de fin est obligatoire.\n");
            endDatePicker.getStyleClass().add("input-error");
        }
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                errors.append("- La date de fin ne peut pas être avant la date de début.\n");
                endDatePicker.getStyleClass().add("input-error");
            }
        }

        try {
            int maxTeams = Integer.parseInt(maxTeamsField.getText());
            if (maxTeams <= 0) {
                errors.append("- Le nombre d'équipes max doit être positif.\n");
                maxTeamsField.getStyleClass().add("input-error");
            }
        } catch (NumberFormatException e) {
            errors.append("- Le nombre d'équipes max doit être un nombre valide.\n");
            maxTeamsField.getStyleClass().add("input-error");
        }

        try {
            int prize = Integer.parseInt(prizeField.getText());
            if (prize < 0) {
                errors.append("- Le prize pool ne peut pas être négatif.\n");
                prizeField.getStyleClass().add("input-error");
            }
        } catch (NumberFormatException e) {
            errors.append("- Le prize pool doit être un nombre valide.\n");
            prizeField.getStyleClass().add("input-error");
        }

        if (gameCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un jeu.\n");
            gameCombo.getStyleClass().add("input-error");
        }
        if (statusCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un statut.\n");
            statusCombo.getStyleClass().add("input-error");
        }
        if (typeCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un type.\n");
            typeCombo.getStyleClass().add("input-error");
        }

        if (placeCombo.getValue() == null || placeCombo.getValue().trim().isEmpty()) {
            errors.append("- Le lieu est obligatoire.\n");
            placeCombo.getStyleClass().add("input-error");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Données invalides");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }
        
        // Smart Capacity Check (Phase 4)
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null && !maxTeamsField.getText().isEmpty()) {
            try {
                long days = java.time.temporal.ChronoUnit.DAYS.between(startDatePicker.getValue(), endDatePicker.getValue()) + 1;
                int teams = Integer.parseInt(maxTeamsField.getText());
                int matchesRequired = teams - 1; // Single elimination assumption
                
                if (days > 0 && (matchesRequired / days) >= 8) {
                    if (smartWarningLabel != null) {
                        smartWarningLabel.setText("⚠️ Attention: Calendrier très dense (" + (matchesRequired/days) + " matchs/jour estimé). Risque de surcharge.");
                        smartWarningLabel.setVisible(true);
                        smartWarningLabel.setManaged(true);
                    }
                } else if (smartWarningLabel != null) {
                    smartWarningLabel.setVisible(false);
                    smartWarningLabel.setManaged(false);
                }
            } catch (Exception e) {
                // Ignore parse errors here, handled above
            }
        }

        return true;
    }

    @FXML
    public void handleDelete() {
        if (tournoi == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Êtes-vous sûr de vouloir supprimer le tournoi " + tournoi.getNom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(tournoi.getId());
                    handleBack();
                } catch (SQLException e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
                }
            }
        });
    }

    @FXML
    public void handleBack() {
        if (mainController != null) {
            mainController.loadView("/com/carthage/view/admin/tournois-view.fxml");
        } else {
            System.err.println("Main controller is null, cannot go back");
        }
    }

    @FXML
    public void handleGoToMatches() {
        if (tournoi == null || mainController == null)
            return;
        // Save current edit state first (optional but safe)
        try {
            TournoiMatchesController matchesController = mainController.loadViewAndGetController(
                    "/com/carthage/view/admin/tournoi-matches-view.fxml");
            if (matchesController != null) {
                matchesController.setTournamentAndMainController(tournoi.getId(), mainController);
            }
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Impossible d'ouvrir la page des matchs: " + e.getMessage()).showAndWait();
        }
    }

    // Converters
    private StringConverter<Game> createGameConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Game o) {
                return o != null ? o.getName() : "";
            }

            @Override
            public Game fromString(String string) {
                return null;
            }
        };
    }

    private StringConverter<User> createUserConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(User o) {
                return o != null ? o.getUsername() : "";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        };
    }

    private StringConverter<Team> createTeamConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Team o) {
                return o != null ? o.getName() : "Non connu";
            }

            @Override
            public Team fromString(String string) {
                return null;
            }
        };
    }
}
