package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.Skin;
import com.carthage.entity.enums.SkinRarity;
import com.carthage.entity.enums.SkinType;
import com.carthage.services.GameService;
import com.carthage.services.SkinService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.concurrent.Task;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class SkinDialogController {

    @FXML private Label titleLabel;
    @FXML private Label errorLabel;

    @FXML private TextField nameField;
    @FXML private TextArea descField;
    @FXML private TextField priceField;
    @FXML private TextField imageField;

    @FXML private ComboBox<Game> gameCombo;
    @FXML private ComboBox<SkinRarity> rarityCombo;

    private final GameService gameService = new GameService();
    private final SkinService skinService = new SkinService();
    private final com.carthage.services.api.CloudinaryService cloudinaryService = new com.carthage.services.api.CloudinaryService();
    private final com.carthage.services.api.SkinportService skinportService = new com.carthage.services.api.SkinportService();
    private Skin currentSkin;

    private Runnable onSaveCallback;
    private Runnable onCancelCallback;

    @FXML
    public void initialize() {
        // Load enums
        rarityCombo.getItems().setAll(SkinRarity.values());

        // Load games
        List<Game> games = gameService.getAllGames();
        gameCombo.getItems().setAll(games);
        gameCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Game game) {
                return game != null ? game.getName() : "";
            }
            @Override
            public Game fromString(String string) {
                return null;
            }
        });
        
        // Defaults
        rarityCombo.setValue(SkinRarity.COMMON);
    }

    public void setSkinData(Skin skin) {
        this.currentSkin = skin;
        if (skin != null) {
            titleLabel.setText("Modifier le Skin");
            nameField.setText(skin.getName());
            descField.setText(skin.getDescription());
            priceField.setText(String.valueOf(skin.getPrice()));
            imageField.setText(skin.getImageUrl());
            rarityCombo.setValue(skin.getRarity());
            
            if (skin.getGame() != null) {
                for (Game g : gameCombo.getItems()) {
                    if (g.getId().equals(skin.getGame().getId())) {
                        gameCombo.setValue(g);
                        break;
                    }
                }
            }
        } else {
            titleLabel.setText("Ajouter un Skin");
        }
    }

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    public void setOnCancelCallback(Runnable onCancelCallback) {
        this.onCancelCallback = onCancelCallback;
    }

    @FXML
    private void onSave() {
        try {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom est obligatoire.");
            }
            if (gameCombo.getValue() == null) {
                throw new IllegalArgumentException("Vous devez sélectionner un jeu.");
            }
            
            int price = 0;
            try {
                price = Integer.parseInt(priceField.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Le prix doit être un nombre valide.");
            }

            boolean isNew = (currentSkin == null);
            if (isNew) {
                currentSkin = new Skin();
                currentSkin.setId(UUID.randomUUID());
            }

            currentSkin.setName(nameField.getText().trim());
            currentSkin.setDescription(descField.getText() != null ? descField.getText().trim() : "");
            currentSkin.setPrice(price);
            currentSkin.setImageUrl(imageField.getText() != null ? imageField.getText().trim() : "");
            currentSkin.setGame(gameCombo.getValue());
            currentSkin.setRarity(rarityCombo.getValue());

            if (isNew) {
                skinService.create(currentSkin);
            } else {
                skinService.update(currentSkin);
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

        } catch (IllegalArgumentException e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Une erreur est survenue lors de l'enregistrement.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour le skin");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(titleLabel.getScene().getWindow());

        if (selectedFile != null) {
            imageField.setText("Upload en cours...");
            imageField.setDisable(true);
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            Task<String> uploadTask = new Task<>() {
                @Override
                protected String call() {
                    return cloudinaryService.uploadImage(selectedFile);
                }
            };

            uploadTask.setOnSucceeded(e -> {
                String url = uploadTask.getValue();
                if (url != null) {
                    imageField.setText(url);
                } else {
                    imageField.setText("");
                    errorLabel.setText("Erreur lors de l'upload de l'image.");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                }
                imageField.setDisable(false);
            });

            uploadTask.setOnFailed(e -> {
                imageField.setText("");
                errorLabel.setText("Erreur réseau: " + uploadTask.getException().getMessage());
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                imageField.setDisable(false);
            });

            new Thread(uploadTask).start();
        }
    }

    @FXML
    private void onFetchSkinportPrice() {
        String skinName = nameField.getText();
        if (skinName == null || skinName.trim().isEmpty()) {
            errorLabel.setText("Veuillez d'abord entrer le nom du skin.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        priceField.setText("Recherche...");
        priceField.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Task<com.carthage.services.api.SkinportService.SkinportItem> fetchTask = new Task<>() {
            @Override
            protected com.carthage.services.api.SkinportService.SkinportItem call() {
                return skinportService.searchItem(skinName.trim());
            }
        };

        fetchTask.setOnSucceeded(e -> {
            com.carthage.services.api.SkinportService.SkinportItem item = fetchTask.getValue();
            if (item != null) {
                priceField.setText(String.valueOf((int) item.getMinPrice()));
            } else {
                priceField.setText("");
                errorLabel.setText("Skin introuvable (Note: Skinport est limité à CS:GO, Rust, Dota 2, TF2).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
            priceField.setDisable(false);
        });

        fetchTask.setOnFailed(e -> {
            priceField.setText("");
            errorLabel.setText("Erreur réseau: " + fetchTask.getException().getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            priceField.setDisable(false);
        });

        new Thread(fetchTask).start();
    }

    @FXML
    private void onCancel() {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }
}
