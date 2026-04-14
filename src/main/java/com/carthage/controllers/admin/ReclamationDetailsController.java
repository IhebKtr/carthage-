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
    @FXML private TextArea replyArea;
    @FXML private ScrollPane scrollPane;

    private final ReclamationService service = new ReclamationService();
    private Reclamation reclamation;
    private AdminMainLayoutController mainController;

    @FXML
    public void initialize() {
        statusCombo.getItems().setAll(ReclamationStatus.values());
        statusCombo.setOnAction(e -> handleStatusChange());
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
                case MEDIUM: priorityLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold;"); break;
                case LOW: priorityLabel.setStyle("-fx-text-fill: #4ADE80; -fx-font-weight: bold;"); break;
            }
        }
        
        dateLabel.setText(reclamation.getCreatedAt() != null ? reclamation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
        statusCombo.setValue(reclamation.getStatus());
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
        } catch (SQLException e) {
            e.printStackTrace();
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
    private void handleBack() {
        if (mainController != null) {
            mainController.loadView("/com/carthage/view/admin/reclamations-view.fxml");
        }
    }
}
