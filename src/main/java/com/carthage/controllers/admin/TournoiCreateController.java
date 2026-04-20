package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.Tournoi;
import com.carthage.entity.User;
import com.carthage.entity.enums.TournamentStatus;
import com.carthage.entity.enums.TournamentType;
import com.carthage.services.TournoiService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.gluonhq.maps.MapLayer;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;

import java.sql.SQLException;
import java.util.List;

public class TournoiCreateController {

    @FXML
    private TextField nameField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextField maxTeamsField;
    @FXML
    private TextField prizeField;
    @FXML
    private ComboBox<Game> gameCombo;
    @FXML
    private ComboBox<TournamentStatus> statusCombo;
    @FXML
    private ComboBox<TournamentType> typeCombo;
    @FXML
    private ComboBox<User> refereeCombo;
    @FXML
    private ComboBox<String> placeCombo;
    @FXML
    private MapView mapWebView;
    
    private MapLayer markerLayer;

    private final TournoiService service = new TournoiService();
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        // Populate and format Game Combo
        List<Game> games = service.getAllGames();
        gameCombo.setItems(FXCollections.observableArrayList(games));
        gameCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Game g) {
                return g != null ? g.getName() : "";
            }

            @Override
            public Game fromString(String s) {
                return null;
            }
        });

        // Referees
        List<User> refs = service.getReferees();
        refereeCombo.setItems(FXCollections.observableArrayList(refs));
        refereeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                return u != null ? u.getUsername() : "";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        // Enums
        statusCombo.setItems(FXCollections.observableArrayList(TournamentStatus.values()));
        statusCombo.setValue(TournamentStatus.UPCOMING);

        typeCombo.setItems(FXCollections.observableArrayList(TournamentType.values()));
        typeCombo.setValue(TournamentType.SINGLE_ELIMINATION);

        // Place list
        placeCombo.setItems(
                FXCollections.observableArrayList("Tunis", "Sousse", "Sfax", "Monastir", "Bizerte", "Esprit, Ghazela"));
        placeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateMap(newVal));
        placeCombo.setValue("Tunis");
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
            case "Sousse":           lat = 35.8254; lon = 10.6369; break;
            case "Sfax":            lat = 34.7406; lon = 10.7603; break;
            case "Monastir":        lat = 35.7780; lon = 10.8262; break;
            case "Bizerte":         lat = 37.2744; lon = 9.8739;  break;
            case "Esprit, Ghazela": lat = 36.8985; lon = 10.1898; break;
            default:                lat = 36.8065; lon = 10.1815; break;
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

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void handleCancel() {
        closeStage();
    }

    @FXML
    public void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Tournoi t = new Tournoi();
            t.setNom(nameField.getText());
            if (startDatePicker.getValue() != null)
                t.setDateDebut(startDatePicker.getValue().atStartOfDay());
            if (endDatePicker.getValue() != null)
                t.setDateFin(endDatePicker.getValue().atStartOfDay());

            t.setNbEquipesMax(Integer.parseInt(maxTeamsField.getText()));
            t.setPrizePool(Integer.parseInt(prizeField.getText()));

            t.setPlace(placeCombo.getValue());
            t.setGame(gameCombo.getValue());
            t.setReferee(refereeCombo.getValue());
            t.setStatus(statusCombo.getValue());
            t.setType(typeCombo.getValue());

            service.create(t);

            if (onSuccessCallback != null)
                onSuccessCallback.run();
            closeStage();

        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de création: " + e.getMessage());
            alert.showAndWait();
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

        // Validate Name
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errors.append("- Le nom du tournoi est obligatoire.\n");
            nameField.getStyleClass().add("input-error");
        }

        // Validate Dates
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
            if (startDatePicker.getValue().isBefore(java.time.LocalDate.now())) {
                errors.append("- La date de début ne peut pas être dans le passé.\n");
                startDatePicker.getStyleClass().add("input-error");
            }
        }

        // Validate Number fields
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

        // Validate Combos
        if (gameCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un jeu.\n");
            gameCombo.getStyleClass().add("input-error");
        }
        if (statusCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un statut.\n");
            statusCombo.getStyleClass().add("input-error");
        }
        if (typeCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un type de tournoi.\n");
            typeCombo.getStyleClass().add("input-error");
        }
        if (refereeCombo.getValue() == null) {
            errors.append("- Veuillez sélectionner un arbitre.\n");
            refereeCombo.getStyleClass().add("input-error");
        }

        // Validate Place
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

        return true;
    }

    private void closeStage() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
