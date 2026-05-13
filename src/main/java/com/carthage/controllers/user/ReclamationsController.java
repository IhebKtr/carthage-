package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.DatabaseConnection;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ReclamationsController {

    // --- Containers ---
    @FXML private StackPane contentStack;
    @FXML private VBox listView;
    @FXML private VBox reclamationsList;

    // --- Form Fields ---
    @FXML private TextField formSubject;
    @FXML private ComboBox<String> formCategory;
    @FXML private ComboBox<String> formPriority;
    @FXML private TextArea formMessage;
    @FXML private Label errorLabel;
    @FXML private Button micBtn;

    private Connection connection;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private boolean isInitialized = false;
    
    private final com.carthage.services.SpeechService speechService = new com.carthage.services.SpeechService();
    private boolean isListening = false;

    @FXML
    public void initialize() {
        if (isInitialized) return;
        connection = DatabaseConnection.getInstance().getConnection();
        
        // If we have the list container, load data
        if (reclamationsList != null) {
            isInitialized = true;
            loadReclamations();
        }
    }

    private void loadReclamations() {
        if (reclamationsList == null) return;
        reclamationsList.getChildren().clear();
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) return;

        String sql =
            "SELECT HEX(r.id) AS id, r.subject, r.message, r.status, r.priority, r.category, r.created_at, " +
            "  (SELECT COUNT(*) FROM reclamation_response rr WHERE rr.reclamation_id = r.id) AS reply_count " +
            "FROM reclamation r " +
            "WHERE r.author_id = UNHEX(REPLACE(?, '-', '')) " +
            "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            boolean any = false;
            while (rs.next()) {
                reclamationsList.getChildren().add(buildCard(
                    rs.getString("id"),
                    rs.getString("subject"),
                    rs.getString("message"),
                    rs.getString("status"),
                    rs.getString("priority"),
                    rs.getString("category"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("reply_count")
                ));
                any = true;
            }
            if (!any) {
                VBox emptyPanel = new VBox(15);
                emptyPanel.setAlignment(Pos.CENTER);
                emptyPanel.setPadding(new Insets(40, 0, 40, 0));
                
                Label emptyIcon = new Label("📋");
                emptyIcon.setStyle("-fx-font-size: 40px; -fx-opacity: 0.3;");
                
                Label emptyText = new Label("Aucune réclamation pour le moment.");
                emptyText.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;");
                
                Button btn = new Button("Ouvrir une nouvelle demande");
                btn.getStyleClass().add("filter-toggle");
                btn.setOnAction(e -> onNewReclamation());
                
                emptyPanel.getChildren().addAll(emptyIcon, emptyText, btn);
                reclamationsList.getChildren().add(emptyPanel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDBError(e.getMessage());
        }
    }

    // --- State Management ---

    @FXML public void onNewReclamation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/reclamations-create-view.fxml"));
            loader.setController(this);
            Node form = loader.load();
            
            // Clear selections and errors
            if (formSubject != null) {
                formSubject.clear();
                formSubject.setStyle("-fx-control-inner-background: #111827; -fx-text-fill: white; -fx-prompt-text-fill: #4B5563;");
            }
            if (formMessage != null) {
                formMessage.clear();
                formMessage.setStyle("-fx-control-inner-background: #111827; -fx-text-fill: white; -fx-prompt-text-fill: #4B5563;");
            }
            if (formCategory != null) formCategory.getSelectionModel().select("OTHER");
            if (formPriority != null) formPriority.getSelectionModel().select("NORMAL");
            if (errorLabel != null) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            }
            
            // Initialize Speech Service for the new form
            if (micBtn != null) {
                micBtn.setText("⏳ Chargement...");
                speechService.initModel(new com.carthage.services.SpeechService.SpeechCallback() {
                    @Override
                    public void onSpeechRecognized(String text) {
                        if (text != null && !text.isEmpty()) {
                            String current = formMessage.getText();
                            formMessage.setText(current + (current.isEmpty() ? "" : " ") + text);
                            formMessage.positionCaret(formMessage.getText().length());
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
            
            listView.setVisible(false);
            contentStack.getChildren().add(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void onCancel() {
        if (isListening) handleToggleDictation(); // Stop listening
        if (contentStack.getChildren().size() > 1) {
            contentStack.getChildren().remove(contentStack.getChildren().size() - 1);
        }
        listView.setVisible(true);
        loadReclamations();
    }

    @FXML public void onSubmitReclamation() {
        String subject = formSubject.getText();
        String category = formCategory.getValue();
        String priority = formPriority.getValue();
        String message = formMessage.getText();

        boolean hasError = false;
        
        // Reset styles
        formSubject.setStyle("-fx-control-inner-background: #111827; -fx-text-fill: white; -fx-prompt-text-fill: #4B5563;");
        formMessage.setStyle("-fx-control-inner-background: #111827; -fx-text-fill: white; -fx-prompt-text-fill: #4B5563;");

        if (subject == null || subject.isBlank()) {
            formSubject.setStyle("-fx-control-inner-background: #111827; -fx-text-fill: white; -fx-prompt-text-fill: #4B5563; -fx-border-color: #FF4D4D; -fx-border-radius: 8; -fx-border-width: 1.5;");
            hasError = true;
        }
        
        if (message == null || message.isBlank()) {
            formMessage.setStyle("-fx-control-inner-background: #111827; -fx-text-fill: white; -fx-prompt-text-fill: #4B5563; -fx-border-color: #FF4D4D; -fx-border-radius: 8; -fx-border-width: 1.5;");
            hasError = true;
        }
        
        if (hasError) {
            if (errorLabel != null) {
                errorLabel.setText("Veuillez remplir les champs obligatoires en rouge.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
            return;
        }
        
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        User user = SessionContext.getInstance().getCurrentUser();
        String recId = UUID.randomUUID().toString().replace("-", "");

        String sql = "INSERT INTO reclamation (id, author_id, subject, category, priority, status, message, created_at, updated_at) " +
                     "VALUES (UNHEX(?), UNHEX(REPLACE(?, '-', '')), ?, ?, ?, 'OPEN', ?, NOW(), NOW())";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, recId);
            ps.setString(2, user.getId().toString());
            ps.setString(3, subject);
            ps.setString(4, category);
            ps.setString(5, priority);
            ps.setString(6, message);
            ps.executeUpdate();
            
            if (isListening) handleToggleDictation(); // Stop listening
            onCancel(); // Success, go back to list
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur DB", "Impossible d'envoyer la demande: " + e.getMessage());
        }
    }

    // --- UI Helpers ---

    private HBox buildCard(String idStr, String subject, String message, String status, String priority,
                            String category, Timestamp createdAt, int replies) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E2633; -fx-background-radius: 12px; -fx-cursor: hand;"));
        card.setOnMouseExited (e -> card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> openDetails(idStr));

        Label iconLabel = new Label("📋");
        iconLabel.setStyle("-fx-font-size: 22px;");

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label subjectLabel = new Label(subject != null ? subject : "(sans titre)");
        subjectLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Status badge
        String stColor = "#F59E0B"; 
        if ("OPEN".equals(status)) stColor = "#22C55E";
        else if ("CLOSED".equals(status)) stColor = "#EF4444";
        Label statusBadge = new Label(status);
        statusBadge.setStyle("-fx-background-color: " + stColor + "22; -fx-text-fill: " + stColor + "; -fx-background-radius: 4px; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");

        titleRow.getChildren().addAll(subjectLabel, statusBadge);

        Label snippet = new Label(message != null ? (message.length() > 80 ? message.substring(0, 80) + "…" : message) : "");
        snippet.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");

        HBox meta = new HBox(14);
        String dateStr = createdAt != null ? createdAt.toLocalDateTime().format(FMT) : "—";
        Label metaLabel = new Label("📅 " + dateStr + "  •  🗂 " + category + "  •  💬 " + replies + " réponses");
        metaLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        meta.getChildren().add(metaLabel);

        info.getChildren().addAll(titleRow, snippet, meta);
        card.getChildren().addAll(iconLabel, info, new Label("›"));
        return card;
    }

    private void showDBError(String msg) {
        Label err = new Label("⚠️ Erreur de connexion : " + msg);
        err.setStyle("-fx-text-fill: -carthage-accent; -fx-padding: 20;");
        reclamationsList.getChildren().add(err);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    public void openDetails(String idStr) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/reclamation-details-view.fxml"));
            Node detailsView = loader.load();
            
            UserReclamationDetailsController detailsController = loader.getController();
            detailsController.setReclamationAndParentController(UUID.fromString(
                idStr.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5")
            ), this);
            
            listView.setVisible(false);
            contentStack.getChildren().add(detailsView);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les détails : " + e.getMessage());
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
                        String current = formMessage.getText();
                        formMessage.setText(current + (current.isEmpty() ? "" : " ") + text);
                        formMessage.positionCaret(formMessage.getText().length());
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
}
