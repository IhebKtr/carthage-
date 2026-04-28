package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.enums.GameStatus;
import com.carthage.entity.enums.GameType;
import com.carthage.services.GameService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.UUID;

public class GameDialogController {

    @FXML private Label headerLabel;
    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private ComboBox<GameType> typeCombo;
    @FXML private ComboBox<GameStatus> statusCombo;
    @FXML private TextField imageUrlField;

    private final GameService gameService = new GameService();
    private Runnable onSuccessCallback;
    private Game gameToEdit;

    @FXML
    public void initialize() {
        typeCombo.getItems().setAll(GameType.values());
        statusCombo.getItems().setAll(GameStatus.values());
        statusCombo.setValue(GameStatus.ACTIVE);
    }

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void setGameForEdit(Game game) {
        this.gameToEdit = game;
        headerLabel.setText("Modifier un Jeu");
        nameField.setText(game.getName());
        descField.setText(game.getDescription());
        typeCombo.setValue(game.getType());
        statusCombo.setValue(game.getStatus());
        imageUrlField.setText(game.getImageUrl());
    }

    @FXML
    public void handleCancel() {
        closeStage();
    }

    @FXML
    public void handleSave() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert("Erreur de validation", "Le nom du jeu est requis.");
            return;
        }

        if (typeCombo.getValue() == null) {
            showAlert("Erreur de validation", "Le type de jeu est requis.");
            return;
        }

        if (gameToEdit == null) {
            // Create
            Game newGame = new Game(
                UUID.randomUUID(),
                nameField.getText().trim(),
                descField.getText() != null ? descField.getText().trim() : "",
                typeCombo.getValue(),
                statusCombo.getValue(),
                imageUrlField.getText() != null ? imageUrlField.getText().trim() : "",
                LocalDateTime.now(),
                null,
                null
            );
            gameService.create(newGame);
        } else {
            // Edit
            gameToEdit.setName(nameField.getText().trim());
            gameToEdit.setDescription(descField.getText() != null ? descField.getText().trim() : "");
            gameToEdit.setType(typeCombo.getValue());
            gameToEdit.setStatus(statusCombo.getValue());
            gameToEdit.setImageUrl(imageUrlField.getText() != null ? imageUrlField.getText().trim() : "");
            gameService.update(gameToEdit);
        }

        if (onSuccessCallback != null) {
            onSuccessCallback.run();
        }
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
