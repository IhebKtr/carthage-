package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Game;
import com.carthagearena.service.MerchService;
import com.carthagearena.service.GameService;
import com.carthagearena.util.MerchValidator;
import com.carthagearena.util.MerchValidator.ValidationResult;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Contrôleur du formulaire Merch (Ajouter / Modifier)
 * Inclut le contrôle de saisie (validation) avec règles avancées
 */
public class MerchFormController implements Initializable {

    // ─── Champs de saisie ────────────────────────────────────────────────────
    @FXML private Label lblTitle;
    @FXML private TextField tfName;
    @FXML private TextArea taDescription;
    @FXML private TextField tfPrice;
    @FXML private TextField tfStock;
    @FXML private TextField tfImageUrl;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<Game> cbGame;
    @FXML private ImageView imagePreview;

    // ─── Labels d'erreur (contrôle de saisie) ────────────────────────────────
    @FXML private Label lblErrorName;
    @FXML private Label lblErrorPrice;
    @FXML private Label lblErrorStock;
    @FXML private Label lblErrorType;

    // ─── Boutons ─────────────────────────────────────────────────────────────
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblGlobalError;

    private Merch currentMerch;
    private Runnable onSaveCallback;
    private final MerchService merchService = new MerchService();
    private final GameService gameService = new GameService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTypeCombo();
        setupGameCombo();
        setupRealTimeValidation();
        setupImagePreview();
        setupNumericInputFilters();
        clearErrors();
    }

    /**
     * Initialise le formulaire avec un Merch existant (modification) ou null (ajout)
     */
    public void initData(Merch merch, Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;

        if (merch != null) {
            this.currentMerch = merch;
            lblTitle.setText("✏️ Modifier le produit");
            populateFields(merch);
        } else {
            lblTitle.setText("➕ Nouveau produit");
        }
    }

    // ─── Initialisation ──────────────────────────────────────────────────────

    private void setupTypeCombo() {
        cbType.setItems(FXCollections.observableArrayList(MerchValidator.getValidTypes()));
        cbType.setPromptText("Choisir un type...");
    }

    private void setupGameCombo() {
        try {
            cbGame.setItems(FXCollections.observableArrayList(gameService.findAll()));
            cbGame.setPromptText("Choisir un jeu (optionnel)...");
        } catch (SQLException e) {
            System.err.println("Erreur de chargement des jeux : " + e.getMessage());
        }
    }

    private void populateFields(Merch merch) {
        tfName.setText(merch.getName());
        taDescription.setText(merch.getDescription());
        tfPrice.setText(String.valueOf(merch.getPrice()));
        tfStock.setText(String.valueOf(merch.getStock()));
        if (merch.getImageUrl() != null) {
            tfImageUrl.setText(merch.getImageUrl());
            updateImagePreview(merch.getImageUrl());
        } else {
            tfImageUrl.setText("");
        }
        cbType.setValue(merch.getType());
        if (merch.getGame() != null) {
            cbGame.setValue(merch.getGame());
        } else {
            cbGame.getSelectionModel().clearSelection();
        }
    }

    // ─── Filtres numériques (empêcher les lettres dans prix/stock) ────────────

    private void setupNumericInputFilters() {
        tfPrice.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                tfPrice.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        tfStock.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                tfStock.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    // ─── Contrôle de saisie en temps réel ────────────────────────────────────

    private void setupRealTimeValidation() {
        // Vérifie le nom au fur et à mesure de la saisie
        tfName.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                showFieldError(lblErrorName, "Le nom du produit est obligatoire");
            } else if (val.trim().length() < 3) {
                showFieldError(lblErrorName, "Min 3 caractères");
            } else if (val.length() > 255) {
                showFieldError(lblErrorName, "Max 255 caractères");
            } else {
                clearFieldError(lblErrorName, tfName);
            }
        });

        // Vérifie le prix : doit être un entier >= 0
        tfPrice.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                showFieldError(lblErrorPrice, "Le prix est obligatoire");
            } else {
                try {
                    int p = Integer.parseInt(val.trim());
                    if (p < 0) {
                        showFieldError(lblErrorPrice, "Le prix doit être ≥ 0");
                    } else {
                        clearFieldError(lblErrorPrice, tfPrice);
                    }
                } catch (NumberFormatException e) {
                    showFieldError(lblErrorPrice, "Valeur numérique requise");
                }
            }
        });

        // Vérifie le stock : doit être un entier >= 0
        tfStock.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                showFieldError(lblErrorStock, "Le stock est obligatoire");
            } else {
                try {
                    int s = Integer.parseInt(val.trim());
                    if (s < 0) {
                        showFieldError(lblErrorStock, "Le stock doit être ≥ 0");
                    } else {
                        clearFieldError(lblErrorStock, tfStock);
                    }
                } catch (NumberFormatException e) {
                    showFieldError(lblErrorStock, "Valeur numérique requise");
                }
            }
        });

        // Vérifie le type
        cbType.valueProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                showFieldError(lblErrorType, "Le type de produit est obligatoire");
            } else {
                clearFieldError(lblErrorType, cbType);
            }
        });
    }

    // ─── Gestion de l'image (URL ou Base64) ──────────────────────────────────

    private void setupImagePreview() {
        tfImageUrl.textProperty().addListener((obs, oldVal, newVal) -> {
            updateImagePreview(newVal);
        });
    }

    private void updateImagePreview(String source) {
        if (source == null || source.isBlank()) {
            imagePreview.setImage(null);
            return;
        }
        try {
            source = source.trim();
            if (source.startsWith("data:image")) {
                // Format: data:image/jpeg;base64,/9j/4AAQ...
                int commaIndex = source.indexOf(",");
                if (commaIndex != -1) {
                    String base64Data = source.substring(commaIndex + 1);
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    imagePreview.setImage(image);
                }
            } else if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file://")) {
                // Lien web classique ou ficher local
                Image image = new Image(source, true); // true = charger en arrière-plan
                imagePreview.setImage(image);
            } else {
                imagePreview.setImage(null);
            }
        } catch (Exception e) {
            // Si l'image est invalide, on n'affiche rien (on ne crashe pas)
            imagePreview.setImage(null);
            System.err.println("Erreur de prévisualisation de l'image : " + e.getMessage());
        }
    }

    // ─── Sauvegarde ──────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        clearErrors();

        // Validation globale avant soumission (avec validation image)
        ValidationResult result = MerchValidator.validateFull(
                tfName.getText(),
                tfPrice.getText(),
                tfStock.getText(),
                cbType.getValue(),
                tfImageUrl.getText()
        );

        // Vérifier la description
        if (!MerchValidator.isDescriptionValid(taDescription.getText())) {
            result.getErrors().add("❌ Description : ne doit pas dépasser 2000 caractères");
        }

        if (!result.isValid()) {
            lblGlobalError.setText(result.getErrorsSummary());
            lblGlobalError.setVisible(true);
            highlightErrors(result);
            return;
        }

        try {
            Merch merch = buildMerchFromForm();

            if (currentMerch == null) {
                merchService.create(merch);
                showSuccessAndClose("Produit ajouté avec succès ✅");
            } else {
                merch.setId(currentMerch.getId());
                merch.setCreatedAt(currentMerch.getCreatedAt());
                merchService.update(merch);
                showSuccessAndClose("Produit modifié avec succès ✅");
            }

            if (onSaveCallback != null) onSaveCallback.run();

        } catch (SQLException e) {
            lblGlobalError.setText("Erreur base de données : " + e.getMessage());
            lblGlobalError.setVisible(true);
        }
    }

    private Merch buildMerchFromForm() {
        Merch merch = new Merch();
        merch.setName(tfName.getText().trim());
        merch.setDescription(taDescription.getText().trim());
        merch.setPrice(Integer.parseInt(tfPrice.getText().trim()));
        merch.setStock(Integer.parseInt(tfStock.getText().trim()));
        merch.setImageUrl(tfImageUrl.getText().trim());
        merch.setType(cbType.getValue());
        if (cbGame.getValue() != null) merch.setGame(cbGame.getValue());
        return merch;
    }

    @FXML
    private void onCancel() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    // ─── Utilitaires UI ──────────────────────────────────────────────────────

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
    }

    private void clearFieldError(Label label, Control field) {
        label.setVisible(false);
        field.setStyle("");
    }

    private void clearErrors() {
        lblErrorName.setVisible(false);
        lblErrorPrice.setVisible(false);
        lblErrorStock.setVisible(false);
        lblErrorType.setVisible(false);
        lblGlobalError.setVisible(false);
        tfName.setStyle("");
        tfPrice.setStyle("");
        tfStock.setStyle("");
        cbType.setStyle("");
    }

    private void highlightErrors(ValidationResult result) {
        String errors = result.getErrorsSummary().toLowerCase();
        if (errors.contains("nom")) tfName.setStyle("-fx-border-color: #FF4D4D;");
        if (errors.contains("prix")) tfPrice.setStyle("-fx-border-color: #FF4D4D;");
        if (errors.contains("stock")) tfStock.setStyle("-fx-border-color: #FF4D4D;");
        if (errors.contains("type")) cbType.setStyle("-fx-border-color: #FF4D4D;");
    }

    private void showSuccessAndClose(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        ((Stage) btnSave.getScene().getWindow()).close();
    }
}
