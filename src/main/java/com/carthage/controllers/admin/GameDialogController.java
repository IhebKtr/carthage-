package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.enums.GameStatus;
import com.carthage.entity.enums.GameType;
import com.carthage.services.GameService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.File;

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
    private final com.carthage.services.api.CloudinaryService cloudinaryService = new com.carthage.services.api.CloudinaryService();
    private final com.carthage.services.api.RawgService rawgService = new com.carthage.services.api.RawgService();
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
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour le jeu");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(headerLabel.getScene().getWindow());

        if (selectedFile != null) {
            imageUrlField.setText("Upload en cours...");
            imageUrlField.setDisable(true);

            Task<String> uploadTask = new Task<>() {
                @Override
                protected String call() {
                    return cloudinaryService.uploadImage(selectedFile);
                }
            };

            uploadTask.setOnSucceeded(e -> {
                String url = uploadTask.getValue();
                if (url != null) {
                    imageUrlField.setText(url);
                } else {
                    imageUrlField.setText("");
                    showAlert("Erreur Cloudinary", "Erreur lors de l'upload de l'image.");
                }
                imageUrlField.setDisable(false);
            });

            uploadTask.setOnFailed(e -> {
                imageUrlField.setText("");
                showAlert("Erreur Réseau", "Erreur: " + uploadTask.getException().getMessage());
                imageUrlField.setDisable(false);
            });

            new Thread(uploadTask).start();
        }
    }

    @FXML
    private void onFetchRawgData() {
        String query = nameField.getText();
        if (query == null || query.trim().isEmpty()) {
            showAlert("Erreur", "Veuillez entrer le nom du jeu pour rechercher.");
            return;
        }

        descField.setText("Recherche en cours sur RAWG...");
        descField.setDisable(true);
        
        Task<com.carthage.services.api.RawgService.RawgGame> fetchTask = new Task<>() {
            @Override
            protected com.carthage.services.api.RawgService.RawgGame call() {
                return rawgService.searchGame(query.trim());
            }
        };

        fetchTask.setOnSucceeded(e -> {
            com.carthage.services.api.RawgService.RawgGame gameInfo = fetchTask.getValue();
            if (gameInfo != null) {
                if (gameInfo.getDescription() != null && !gameInfo.getDescription().equals("No description available.")) {
                    descField.setText(gameInfo.getDescription());
                } else {
                    descField.setText("");
                }
                if (gameInfo.getBackgroundImage() != null) {
                    imageUrlField.setText(gameInfo.getBackgroundImage());
                }
                nameField.setText(gameInfo.getName());
            } else {
                descField.setText("");
                showAlert("Non trouvé", "Ce jeu n'a pas été trouvé sur RAWG.");
            }
            descField.setDisable(false);
        });

        fetchTask.setOnFailed(e -> {
            descField.setText("");
            showAlert("Erreur Réseau", "Impossible de contacter l'API RAWG.");
            descField.setDisable(false);
        });

        new Thread(fetchTask).start();
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
