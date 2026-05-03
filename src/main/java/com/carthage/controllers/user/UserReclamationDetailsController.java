package com.carthage.controllers.user;

import com.carthage.entity.Reclamation;
import com.carthage.entity.ReclamationResponse;
import com.carthage.entity.enums.ReclamationStatus;
import com.carthage.services.ReclamationService;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class UserReclamationDetailsController {

    @FXML private Label subjectLabel;
    @FXML private Label authorLabel;
    @FXML private Label statusBadge;
    
    @FXML private Label originalAuthorLabel;
    @FXML private Label originalDateLabel;
    @FXML private Label messageText;
    
    @FXML private Label categoryLabel;
    @FXML private Label priorityLabel;
    @FXML private Label dateLabel;
    
    @FXML private VBox conversationBox;
    @FXML private VBox replyBox;
    @FXML private VBox resolvedBanner;
    @FXML private TextArea replyArea;
    @FXML private ScrollPane scrollPane;
    @FXML private Button micBtn;

    private final ReclamationService service = new ReclamationService();
    private final com.carthage.services.SpeechService speechService = new com.carthage.services.SpeechService();
    private boolean isListening = false;
    private Reclamation reclamation;
    private ReclamationsController parentController;
    private final com.carthage.services.TranslationService translationService = new com.carthage.services.TranslationService();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Initialize Speech Service
        speechService.initModel(new com.carthage.services.SpeechService.SpeechCallback() {
            @Override
            public void onSpeechRecognized(String text) {
                if (text != null && !text.isEmpty()) {
                    String current = replyArea.getText();
                    replyArea.setText(current + (current.isEmpty() ? "" : " ") + text);
                    replyArea.positionCaret(replyArea.getText().length());
                }
            }
            @Override
            public void onStatusUpdate(String status) {
                if (micBtn != null) micBtn.setText(status.equals("Prêt !") ? "🎤 Dictée" : "⏳ " + status);
            }
            @Override
            public void onError(String error) {
                if (micBtn != null) {
                    micBtn.setText("❌ Erreur");
                    micBtn.setStyle("-fx-text-fill: #FF4D4D; -fx-border-color: #FF4D4D; -fx-background-color: transparent; -fx-border-radius: 12;");
                }
            }
        });
    }

    public void setReclamationAndParentController(UUID id, ReclamationsController parentController) {
        this.parentController = parentController;
        loadReclamation(id);
    }

    private void loadReclamation(UUID id) {
        reclamation = service.getById(id);
        if (reclamation == null) {
            handleBack();
            return;
        }

        subjectLabel.setText(reclamation.getSubject() != null ? reclamation.getSubject() : "Sans titre");
        
        statusBadge.setText(reclamation.getStatus() != null ? reclamation.getStatus().name() : "");
        String stColor = "#F59E0B"; 
        if (reclamation.getStatus() == ReclamationStatus.OPEN) stColor = "#22C55E";
        else if (reclamation.getStatus() == ReclamationStatus.CLOSED || reclamation.getStatus() == ReclamationStatus.RESOLVED) stColor = "#EF4444";
        statusBadge.setStyle("-fx-background-color: " + stColor + "22; -fx-text-fill: " + stColor + "; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 12px;");

        categoryLabel.setText(reclamation.getCategory() != null ? reclamation.getCategory().name() : "");
        priorityLabel.setText(reclamation.getPriority() != null ? reclamation.getPriority().name() : "");
        dateLabel.setText(reclamation.getCreatedAt() != null ? reclamation.getCreatedAt().format(FMT) : "");

        originalDateLabel.setText(dateLabel.getText());
        messageText.setText(reclamation.getMessage());

        updateConversation();
        updateUIBasedOnStatus();
    }

    private void updateConversation() {
        // keep only the first child (the original message)
        if (conversationBox.getChildren().size() > 1) {
            conversationBox.getChildren().remove(1, conversationBox.getChildren().size());
        }

        List<ReclamationResponse> responses = service.getResponses(reclamation.getId());
        for (ReclamationResponse response : responses) {
            boolean isAdmin = response.isIsAdminResponse();
            VBox msgBox = new VBox(8);
            msgBox.setStyle("-fx-background-color: " + (isAdmin ? "rgba(229, 9, 20, 0.05)" : "#2A3441") + ";" +
                            "-fx-padding: 15; -fx-background-radius: 12; -fx-border-color: " + (isAdmin ? "rgba(229, 9, 20, 0.2)" : "#374151") + ";" +
                            "-fx-border-radius: 12;");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label nameLbl = new Label(isAdmin ? "Support Admin" : "Vous");
            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isAdmin ? "#E50914" : "white") + ";");
            
            Label dateLbl = new Label(response.getCreatedAt() != null ? response.getCreatedAt().format(FMT) : "");
            dateLbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");

            header.getChildren().addAll(nameLbl, dateLbl);

            Label txt = new Label(response.getMessage());
            txt.setWrapText(true);
            txt.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

            Button trFrBtn = new Button("🌐 FR");
            trFrBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0;");
            trFrBtn.setOnAction(e -> translateText(txt, "fr", trFrBtn));

            Button trEnBtn = new Button("🌐 EN");
            trEnBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0;");
            trEnBtn.setOnAction(e -> translateText(txt, "en", trEnBtn));

            HBox actionsBox = new HBox(10, trFrBtn, trEnBtn);
            actionsBox.setAlignment(Pos.CENTER_LEFT);

            msgBox.getChildren().addAll(header, txt, actionsBox);
            conversationBox.getChildren().add(msgBox);
        }

        // Scroll to bottom
        javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void updateUIBasedOnStatus() {
        boolean isClosed = reclamation.getStatus() == ReclamationStatus.CLOSED || reclamation.getStatus() == ReclamationStatus.RESOLVED;
        replyBox.setVisible(!isClosed);
        replyBox.setManaged(!isClosed);
        resolvedBanner.setVisible(isClosed);
        resolvedBanner.setManaged(isClosed);
    }

    @FXML
    private void handleSendReply() {
        String replyText = replyArea.getText();
        if (replyText == null || replyText.trim().isEmpty()) {
            return;
        }

        ReclamationResponse response = new ReclamationResponse();
        response.setReclamation(reclamation);
        response.setAuthor(SessionContext.getInstance().getCurrentUser());
        response.setMessage(replyText.trim());
        response.setIsAdminResponse(false);

        try {
            service.addResponse(response);
            
            replyArea.clear();
            if (isListening) handleToggleDictation();
            
            loadReclamation(reclamation.getId());
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleToggleDictation() {
        if (!isListening) {
            isListening = true;
            if (micBtn != null) {
                micBtn.setStyle("-fx-background-color: rgba(229,9,20,0.2); -fx-text-fill: #FF4D4D; -fx-border-color: #FF4D4D; -fx-border-radius: 12;");
                micBtn.setText("🔴 Écoute...");
            }
            speechService.startListening(new com.carthage.services.SpeechService.SpeechCallback() {
                @Override
                public void onSpeechRecognized(String text) {
                    if (text != null && !text.isEmpty()) {
                        String current = replyArea.getText();
                        replyArea.setText(current + (current.isEmpty() ? "" : " ") + text);
                        replyArea.positionCaret(replyArea.getText().length());
                    }
                }
                @Override
                public void onStatusUpdate(String status) {}
                @Override
                public void onError(String error) {
                    handleToggleDictation();
                }
            });
        } else {
            isListening = false;
            speechService.stopListening();
            if (micBtn != null) {
                micBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; -fx-border-radius: 12;");
                micBtn.setText("🎤 Dictée");
            }
        }
    }

    @FXML
    private void handleEmojiClick(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Button) {
            Button btn = (Button) event.getSource();
            String emoji = btn.getText();
            String current = replyArea.getText();
            replyArea.setText(current + emoji);
            replyArea.positionCaret(replyArea.getText().length());
            replyArea.requestFocus();
        }
    }

    @FXML
    private void translateOriginalToFr(javafx.event.ActionEvent event) {
        translateText(messageText, "fr", (Button) event.getSource());
    }

    @FXML
    private void translateOriginalToEn(javafx.event.ActionEvent event) {
        translateText(messageText, "en", (Button) event.getSource());
    }

    private void translateText(Label label, String lang, Button btn) {
        String originalText = label.getText();
        String oldBtnText = btn.getText();
        btn.setText("⏳...");
        btn.setDisable(true);
        new Thread(() -> {
            String translated = translationService.translate(originalText, lang);
            javafx.application.Platform.runLater(() -> {
                label.setText(translated);
                btn.setText("✅ " + lang.toUpperCase());
                btn.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void handleBack() {
        if (isListening) handleToggleDictation();
        if (parentController != null) {
            parentController.onCancel();
        }
    }
}
