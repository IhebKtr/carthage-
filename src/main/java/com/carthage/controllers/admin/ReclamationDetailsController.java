package com.carthage.controllers.admin;

import com.carthage.entity.Reclamation;
import com.carthage.entity.ReclamationResponse;
import com.carthage.entity.User;
import com.carthage.entity.enums.ReclamationStatus;
import com.carthage.services.ReclamationService;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class ReclamationDetailsController {

    @FXML private Label subjectLabel;
    @FXML private Label authorLabel;
    @FXML private Label dateLabel;
    @FXML private Label categoryLabel;
    @FXML private Label priorityLabel;
    @FXML private Label originalAuthorLabel;
    @FXML private Label originalDateLabel;
    @FXML private Label messageText;
    @FXML private ComboBox<ReclamationStatus> statusCombo;
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
    private AdminMainLayoutController mainController;

    @FXML
    public void initialize() {
        statusCombo.getItems().setAll(ReclamationStatus.values());
        statusCombo.setOnAction(e -> handleStatusChange());
        
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
                micBtn.setText(status.equals("Prêt !") ? "🎤 Dictée" : "⏳ " + status);
            }
            @Override
            public void onError(String error) {
                micBtn.setText("❌ Erreur");
                micBtn.setStyle("-fx-text-fill: #FF4D4D; -fx-border-color: #FF4D4D; -fx-background-color: transparent; -fx-border-radius: 12;");
                System.err.println("Speech error: " + error);
            }
        });
    }

    public void setReclamationAndMainController(UUID id, AdminMainLayoutController mainController) {
        this.mainController = mainController;
        this.reclamation = service.getById(id);
        if (reclamation != null) {
            populateFields();
            loadResponses();
        }
    }

    private void populateFields() {
        subjectLabel.setText(reclamation.getSubject());
        String authorEmail = reclamation.getAuthor() != null ? reclamation.getAuthor().getEmail() : "Inconnu";
        String dateStr = reclamation.getCreatedAt() != null ? reclamation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
        
        authorLabel.setText("Par " + authorEmail + " • " + dateStr);
        originalAuthorLabel.setText(reclamation.getAuthor() != null ? reclamation.getAuthor().getUsername() : "Utilisateur");
        originalDateLabel.setText(dateStr);
        messageText.setText(reclamation.getMessage());
        
        categoryLabel.setText(reclamation.getCategory() != null ? reclamation.getCategory().name() : "N/A");
        priorityLabel.setText(reclamation.getPriority() != null ? reclamation.getPriority().name() : "N/A");
        if (reclamation.getPriority() != null) {
            switch (reclamation.getPriority()) {
                case HIGH: priorityLabel.setStyle("-fx-text-fill: #FF4D4D; -fx-font-weight: bold;"); break;
                case URGENT: priorityLabel.setStyle("-fx-text-fill: #FF4D4D; -fx-font-weight: bold;"); break;
                case MEDIUM: priorityLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold;"); break;
                case LOW: priorityLabel.setStyle("-fx-text-fill: #4ADE80; -fx-font-weight: bold;"); break;
            }
        }
        dateLabel.setText(reclamation.getCreatedAt() != null ? reclamation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        statusCombo.setValue(reclamation.getStatus());
        updateReplyState(reclamation.getStatus());
    }

    private void loadResponses() {
        // Clear all except the first message box (which is the original message)
        if (conversationBox.getChildren().size() > 1) {
            conversationBox.getChildren().remove(1, conversationBox.getChildren().size());
        }
        
        List<ReclamationResponse> responses = service.getResponses(reclamation.getId());
        for (ReclamationResponse resp : responses) {
            addResponseBubble(resp);
        }
    }

    private void addResponseBubble(ReclamationResponse resp) {
        VBox bubble = new VBox(5);
        bubble.getStyleClass().add("response-bubble");
        
        boolean isAdmin = resp.isIsAdminResponse();
        if (isAdmin) {
            bubble.setStyle("-fx-background-color: rgba(229, 9, 20, 0.1); -fx-border-color: rgba(229, 9, 20, 0.3); -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 15;");
            bubble.setAlignment(Pos.TOP_RIGHT);
        } else {
            bubble.setStyle("-fx-background-color: #1E2633; -fx-border-color: #2A3441; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 15;");
            bubble.setAlignment(Pos.TOP_LEFT);
        }

        HBox meta = new HBox(10);
        meta.setAlignment(isAdmin ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        Label name = new Label(resp.getAuthor() != null ? resp.getAuthor().getUsername() : (isAdmin ? "Admin" : "User"));
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isAdmin ? "-carthage-accent;" : "white;"));
        
        Label date = new Label(resp.getCreatedAt() != null ? resp.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        date.getStyleClass().add("stat-label-mini");
        
        if (isAdmin) meta.getChildren().addAll(date, name);
        else meta.getChildren().addAll(name, date);

        Label msg = new Label(resp.getMessage());
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: white;");
        
        bubble.getChildren().addAll(meta, msg);
        conversationBox.getChildren().add(bubble);
        
        // Scroll to bottom
        scrollPane.setVvalue(1.0);
    }

    @FXML
    private void handleStatusChange() {
        if (reclamation == null || statusCombo.getValue() == null) return;
        try {
            service.updateStatus(reclamation.getId(), statusCombo.getValue());
            reclamation.setStatus(statusCombo.getValue());
            updateReplyState(statusCombo.getValue());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Show reply box or resolved banner depending on current status. */
    private void updateReplyState(ReclamationStatus status) {
        boolean isResolved = status == ReclamationStatus.RESOLVED || status == ReclamationStatus.CLOSED;
        replyBox.setVisible(!isResolved);
        replyBox.setManaged(!isResolved);
        resolvedBanner.setVisible(isResolved);
        resolvedBanner.setManaged(isResolved);
    }

    @FXML
    private void handleMarkResolved() {
        if (reclamation == null) return;
        try {
            service.updateStatus(reclamation.getId(), ReclamationStatus.RESOLVED);
            reclamation.setStatus(ReclamationStatus.RESOLVED);
            statusCombo.setValue(ReclamationStatus.RESOLVED);
            updateReplyState(ReclamationStatus.RESOLVED);
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleReopenTicket() {
        if (reclamation == null) return;
        try {
            service.updateStatus(reclamation.getId(), ReclamationStatus.OPEN);
            reclamation.setStatus(ReclamationStatus.OPEN);
            statusCombo.setValue(ReclamationStatus.OPEN);
            updateReplyState(ReclamationStatus.OPEN);
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleSendReply() {
        String text = replyArea.getText();
        if (text == null || text.trim().isEmpty()) return;

        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) {
             new Alert(Alert.AlertType.ERROR, "Non connecté.").show();
             return;
        }

        ReclamationResponse resp = new ReclamationResponse();
        resp.setMessage(text);
        resp.setIsAdminResponse(true);
        resp.setReclamation(reclamation);
        resp.setAuthor(currentUser);

        try {
            service.addResponse(resp);
            replyArea.clear();
            loadResponses();
            
            // Auto update status to OPEN if it was PENDING
            if (reclamation.getStatus() == ReclamationStatus.PENDING) {
                statusCombo.setValue(ReclamationStatus.OPEN);
                handleStatusChange();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement ce ticket ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(reclamation.getId());
                    handleBack();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleToggleDictation() {
        if (!isListening) {
            isListening = true;
            micBtn.setStyle("-fx-background-color: rgba(229,9,20,0.2); -fx-text-fill: #FF4D4D; -fx-border-color: #FF4D4D; -fx-border-radius: 12;");
            micBtn.setText("🔴 Écoute...");
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
                public void onStatusUpdate(String status) {
                    // Update text if needed
                }
                @Override
                public void onError(String error) {
                    handleToggleDictation(); // Stop if error
                    System.err.println("Dictation error: " + error);
                }
            });
        } else {
            isListening = false;
            speechService.stopListening();
            micBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #9CA3AF; -fx-border-color: #4B5563; -fx-border-radius: 12;");
            micBtn.setText("🎤 Dictée");
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
    private void handleBack() {
        if (isListening) handleToggleDictation(); // Stop listening
        if (mainController != null) {
            mainController.loadView("/com/carthage/view/admin/reclamations-view.fxml");
        }
    }
}
