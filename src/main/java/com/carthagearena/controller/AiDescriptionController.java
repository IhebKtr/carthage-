package com.carthagearena.controller;

import com.carthagearena.model.Merch;
import com.carthagearena.service.AiService;
import com.carthagearena.service.MerchService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur de génération de description IA (Groq / LLaMA 3.3)
 * Équivalent de la route /admin/api/generate-merch-description de Symfony
 */
public class AiDescriptionController implements Initializable {

    @FXML private ComboBox<Merch> cbMerch;
    @FXML private TextField tfName;
    @FXML private TextField tfType;
    @FXML private TextField tfPrice;
    @FXML private TextArea taGeneratedDescription;
    @FXML private Button btnGenerate;
    @FXML private Button btnApply;
    @FXML private ProgressIndicator spinner;
    @FXML private Label lblStatus;
    @FXML private Label lblFraudScore;
    @FXML private Label lblDynamicPrice;
    @FXML private VBox panelResult;

    private final AiService aiService   = new AiService();
    private final MerchService merchService = new MerchService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadMerchs();
        spinner.setVisible(false);
        panelResult.setVisible(false);

        // Auto-remplissage quand on sélectionne un Merch
        cbMerch.valueProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                tfName.setText(selected.getName());
                tfType.setText(selected.getType());
                tfPrice.setText(String.valueOf(selected.getPrice()));

                // Calcul fraud score et prix dynamique à la sélection
                double fraud   = aiService.fraudScore(1, selected.getPrice());
                double dynPrice = aiService.calculateDynamicPrice(selected.getPrice(), selected.getStock());
                lblFraudScore.setText("Score fraude : " + aiService.fraudScoreLabel(fraud));
                lblDynamicPrice.setText(String.format("Prix dynamique : %.2f DT", dynPrice / 100.0));
            }
        });
    }

    private void loadMerchs() {
        try {
            List<Merch> list = merchService.findAll();
            cbMerch.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            showError("Erreur chargement produits", e.getMessage());
        }
    }

    // ─── Générer la description ───────────────────────────────────────────────

    @FXML
    private void onGenerate() {
        String name  = tfName.getText().trim();
        String type  = tfType.getText().trim();
        String priceStr = tfPrice.getText().trim();

        if (name.isBlank() || type.isBlank() || priceStr.isBlank()) {
            showError("Champs requis", "Veuillez remplir le nom, le type et le prix.");
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceStr);
        } catch (NumberFormatException e) {
            showError("Prix invalide", "Le prix doit être un nombre entier.");
            return;
        }

        // Appel API dans un thread séparé (ne pas bloquer le thread JavaFX)
        setLoading(true);
        lblStatus.setText("⏳ Génération en cours via Groq AI...");

        final int finalPrice = price;
        Thread thread = new Thread(() -> {
            try {
                String description = aiService.generateMerchDescription(name, type, finalPrice);

                // Retour sur le thread JavaFX
                Platform.runLater(() -> {
                    taGeneratedDescription.setText(description);
                    panelResult.setVisible(true);
                    lblStatus.setText("✅ Description générée avec succès !");
                    setLoading(false);
                });

            } catch (RuntimeException e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Erreur : " + e.getMessage());
                    setLoading(false);
                    showError("Erreur Groq AI", e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ─── Appliquer la description générée au produit ──────────────────────────

    @FXML
    private void onApply() {
        Merch selected = cbMerch.getValue();
        if (selected == null) {
            showError("Aucun produit sélectionné",
                    "Sélectionnez un produit pour lui appliquer la description.");
            return;
        }

        String generated = taGeneratedDescription.getText().trim();
        if (generated.isBlank()) {
            showError("Aucune description", "Générez d'abord une description.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Appliquer la description");
        confirm.setHeaderText("Remplacer la description de « " + selected.getName() + " » ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                selected.setDescription(generated);
                try {
                    merchService.update(selected);
                    lblStatus.setText("✅ Description sauvegardée dans la base de données !");
                } catch (SQLException e) {
                    showError("Erreur", e.getMessage());
                }
            }
        });
    }

    // ─── Utilitaires UI ──────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        spinner.setVisible(loading);
        btnGenerate.setDisable(loading);
        btnApply.setDisable(loading);
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
