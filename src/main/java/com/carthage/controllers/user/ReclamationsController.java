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

    private Connection connection;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private boolean isInitialized = false;

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
            "SELECT r.subject, r.message, r.status, r.priority, r.category, r.created_at, " +
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
            
            // Clear selections
            if (formCategory != null) formCategory.getSelectionModel().select("OTHER");
            if (formPriority != null) formPriority.getSelectionModel().select("NORMAL");
            
            listView.setVisible(false);
            contentStack.getChildren().add(form);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void onCancel() {
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

        if (subject == null || subject.isBlank() || message == null || message.isBlank()) {
            showAlert("Erreur", "Veuillez remplir les champs obligatoires (*)");
            return;
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
            
            onCancel(); // Success, go back to list
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur DB", "Impossible d'envoyer la demande: " + e.getMessage());
        }
    }

    // --- UI Helpers ---

    private HBox buildCard(String subject, String message, String status, String priority,
                            String category, Timestamp createdAt, int replies) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E2633; -fx-background-radius: 12px; -fx-cursor: hand;"));
        card.setOnMouseExited (e -> card.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12px; -fx-cursor: hand;"));

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
}
