package com.carthage.controllers.admin;

import com.carthage.entity.Game;
import com.carthage.entity.Skin;
import com.carthage.entity.enums.SkinRarity;
import com.carthage.entity.enums.SkinType;
import com.carthage.services.GameService;
import com.carthage.services.SkinService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

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
    private void onCancel() {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }
}
