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

import java.sql.SQLException;
import java.util.List;

public class TournoiCreateController {

    @FXML private TextField nameField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField maxTeamsField;
    @FXML private TextField prizeField;
    @FXML private ComboBox<Game> gameCombo;
    @FXML private ComboBox<TournamentStatus> statusCombo;
    @FXML private ComboBox<TournamentType> typeCombo;
    @FXML private ComboBox<User> refereeCombo;
    @FXML private TextField placeField;

    private final TournoiService service = new TournoiService();
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        // Populate and format Game Combo
        List<Game> games = service.getAllGames();
        gameCombo.setItems(FXCollections.observableArrayList(games));
        gameCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Game g) { return g != null ? g.getName() : ""; }
            @Override public Game fromString(String s) { return null; }
        });

        // Referees
        List<User> refs = service.getReferees();
        refereeCombo.setItems(FXCollections.observableArrayList(refs));
        refereeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(User u) { return u != null ? u.getUsername() : ""; }
            @Override public User fromString(String string) { return null; }
        });

        // Enums
        statusCombo.setItems(FXCollections.observableArrayList(TournamentStatus.values()));
        statusCombo.setValue(TournamentStatus.UPCOMING);
        
        typeCombo.setItems(FXCollections.observableArrayList(TournamentType.values()));
        typeCombo.setValue(TournamentType.SINGLE_ELIMINATION);
    }

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void handleCancel() {
        if (confirmCloseIfDirty()) {
            closeStage();
        }
    }

    /**
     * Returns true if the admin has typed/selected anything beyond the initial
     * defaults. The form opens with all text fields empty, both date pickers
     * empty, both combos for game/referee empty, and statusCombo + typeCombo
     * preselected (UPCOMING / SINGLE_ELIMINATION) — so any deviation from those
     * means the user has work that would be lost on close.
     */
    public boolean isDirty() {
        if (!safe(nameField.getText()).isEmpty())     return true;
        if (!safe(maxTeamsField.getText()).isEmpty()) return true;
        if (!safe(prizeField.getText()).isEmpty())    return true;
        if (!safe(placeField.getText()).isEmpty())    return true;
        if (startDatePicker.getValue() != null)       return true;
        if (endDatePicker.getValue() != null)         return true;
        if (gameCombo.getValue() != null)             return true;
        if (refereeCombo.getValue() != null)          return true;
        if (statusCombo.getValue() != TournamentStatus.UPCOMING)             return true;
        if (typeCombo.getValue()   != TournamentType.SINGLE_ELIMINATION)     return true;
        return false;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * Clean form: returns true (proceed to close, no prompt).
     * Dirty form: shows a confirmation; true = discard & close, false = stay open.
     */
    public boolean confirmCloseIfDirty() {
        if (!isDirty()) return true;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Modifications non enregistrées");
        confirm.setHeaderText(null);
        confirm.setContentText("Vous avez des modifications non enregistrées. Voulez-vous vraiment fermer sans enregistrer ?");
        ButtonType discard = new ButtonType("Abandonner les modifications");
        ButtonType keep    = new ButtonType("Continuer l'édition", ButtonType.CANCEL.getButtonData());
        confirm.getButtonTypes().setAll(discard, keep);

        Stage owner = (Stage) nameField.getScene().getWindow();
        if (owner != null) confirm.initOwner(owner);

        return confirm.showAndWait().filter(b -> b == discard).isPresent();
    }

    @FXML
    public void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Tournoi t = new Tournoi();
            t.setNom(nameField.getText());
            if (startDatePicker.getValue() != null) t.setDateDebut(startDatePicker.getValue().atStartOfDay());
            if (endDatePicker.getValue() != null) t.setDateFin(endDatePicker.getValue().atStartOfDay());
            
            t.setNbEquipesMax(Integer.parseInt(maxTeamsField.getText()));
            t.setPrizePool(Integer.parseInt(prizeField.getText()));
            
            t.setPlace(placeField.getText());
            t.setGame(gameCombo.getValue());
            t.setReferee(refereeCombo.getValue());
            t.setStatus(statusCombo.getValue());
            t.setType(typeCombo.getValue());

            service.create(t);

            if (onSuccessCallback != null) onSuccessCallback.run();
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
        placeField.getStyleClass().remove("input-error");

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
        if (placeField.getText() == null || placeField.getText().trim().isEmpty()) {
            errors.append("- Le lieu est obligatoire.\n");
            placeField.getStyleClass().add("input-error");
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
