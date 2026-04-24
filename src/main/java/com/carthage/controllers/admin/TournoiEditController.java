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
    private TextField placeField;

    @FXML
    private Label infoStartDate;
    @FXML
    private Label infoParticipants;

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
                } else {
                    setText(item.getName() != null ? item.getName() : "Unknown Team");
                    setStyle("-fx-text-fill: white; -fx-padding: 8;");
                }
            }
        });
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
        placeField.setText(tournoi.getPlace());

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

        winnerCombo.setItems(FXCollections.observableArrayList(participatingTeams));
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

        tournoi.setPlace(placeField.getText());

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
        placeField.getStyleClass().remove("input-error");

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
