package com.carthage.controllers.admin;

import com.carthage.entity.MatchEntity;
import com.carthage.entity.Team;
import com.carthage.entity.Tournoi;
import com.carthage.entity.enums.MatchStatus;
import com.carthage.services.MatchService;
import com.carthage.services.TournoiService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TournoiMatchesController {

    // ── Header ──
    @FXML private Label headerTitleLabel;
    @FXML private Label headerSubLabel;

    // ── Configuration Panel ──
    @FXML private DatePicker firstMatchDatePicker;
    @FXML private TextField minutesBetweenField;

    // ── Info Panel ──
    @FXML private Label infoTeamsLabel;
    @FXML private Label infoTypeLabel;
    @FXML private Label infoMatchCountLabel;
    @FXML private Label statusWarningLabel;

    // ── Bracket/List Display ──
    @FXML private VBox matchesContainer;
    @FXML private ScrollPane matchesScrollPane;
    @FXML private Label emptyStateLabel;

    private final TournoiService tournoiService = new TournoiService();
    private final MatchService matchService = new MatchService();

    private Tournoi tournoi;
    private List<Team> teams;
    private AdminMainLayoutController mainController;
    private List<MatchEntity> currentMatches;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        firstMatchDatePicker.setValue(LocalDate.now());
        minutesBetweenField.setText("90");
    }

    // ─── Wiring ──────────────────────────────────────────────────────

    public void setTournamentAndMainController(UUID tournoiId, AdminMainLayoutController mainController) {
        this.mainController = mainController;
        this.tournoi = tournoiService.getById(tournoiId);
        if (tournoi == null) return;

        this.teams = tournoiService.getTeamsForTournament(tournoiId);

        // Update header
        headerTitleLabel.setText("Créer les Matchs — " + tournoi.getNom());
        headerSubLabel.setText("Générer le tableau de rencontres pour ce tournoi");

        // Update info panel
        infoTeamsLabel.setText(teams.size() + " équipes inscrites");
        infoTypeLabel.setText(tournoi.getType() != null ? tournoi.getType().name().replace("_", " ") : "—");

        int expectedCount = estimateMatchCount();
        infoMatchCountLabel.setText(expectedCount + " matchs à créer");

        // Check if matches already exist
        boolean hasMatches = matchService.hasMatches(tournoiId);
        if (hasMatches) {
            statusWarningLabel.setText("⚠ Ce tournoi possède déjà des matchs. Cliquer sur \"Créer les Matchs\" les régénèrera.");
            statusWarningLabel.setVisible(true);
        } else {
            statusWarningLabel.setVisible(false);
        }

        // Show existing matches
        loadExistingMatches();
    }

    // ─── Bracket Estimation ──────────────────────────────────────────

    private int estimateMatchCount() {
        if (teams == null || teams.isEmpty()) return 0;
        int n = teams.size();
        if (tournoi.getType() == null) return n - 1;
        return switch (tournoi.getType()) {
            case ROUND_ROBIN -> (n * (n - 1)) / 2;
            default -> n / 2; // Round 1 only for elimination
        };
    }

    // ─── Action: Create Matches ──────────────────────────────────────

    @FXML
    public void handleCreateMatches() {
        if (tournoi == null) return;

        if (teams == null || teams.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucune équipe inscrite dans ce tournoi.").showAndWait();
            return;
        }
        if (teams.size() < 2) {
            new Alert(Alert.AlertType.WARNING, "Il faut au minimum 2 équipes pour créer des matchs.").showAndWait();
            return;
        }

        // Validate inputs
        LocalDate dateVal = firstMatchDatePicker.getValue();
        if (dateVal == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner la date du premier match.").showAndWait();
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesBetweenField.getText().trim());
            if (minutes < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Veuillez saisir un intervalle de temps valide (en minutes).").showAndWait();
            return;
        }

        // Confirm regeneration if matches exist
        if (matchService.hasMatches(tournoi.getId())) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Des matchs existent déjà pour ce tournoi.\nVoulez-vous les supprimer et régénérer le tableau ?");
            confirm.setHeaderText("Régénérer les matchs ?");
            var result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;

            try {
                matchService.deleteMatchesByTournament(tournoi.getId());
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression des anciens matchs: " + e.getMessage()).showAndWait();
                return;
            }
        }

        LocalDateTime firstMatchDateTime = dateVal.atTime(10, 0); // Start at 10:00

        try {
            List<MatchEntity> created = matchService.generateAndSaveMatches(tournoi, teams, firstMatchDateTime, minutes);

            // Update info
            statusWarningLabel.setText("✅ " + created.size() + " match(s) générés avec succès !");
            statusWarningLabel.setStyle("-fx-text-fill: #4ADE80; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-color: rgba(74,222,128,0.1); -fx-background-radius: 8;");
            statusWarningLabel.setVisible(true);

            int expectedCount = estimateMatchCount();
            infoMatchCountLabel.setText(created.size() + " matchs créés");

            // Reload display
            loadExistingMatches();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la création des matchs: " + e.getMessage()).showAndWait();
        }
    }

    // ─── Load & Display Existing Matches ────────────────────────────

    private void loadExistingMatches() {
        matchesContainer.getChildren().clear();

        currentMatches = matchService.getMatchesByTournament(tournoi.getId());

        if (currentMatches.isEmpty()) {
            emptyStateLabel.setVisible(true);
            matchesScrollPane.setVisible(false);
            return;
        }

        emptyStateLabel.setVisible(false);
        matchesScrollPane.setVisible(true);

        // Group by round
        int currentRound = -1;
        for (MatchEntity m : currentMatches) {
            if (m.getRound() != currentRound) {
                currentRound = m.getRound();
                Label roundLabel = new Label("ROUND " + currentRound);
                roundLabel.setStyle("-fx-text-fill: #E50914; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 15 5 5 5;");
                matchesContainer.getChildren().add(roundLabel);
            }
            matchesContainer.getChildren().add(createMatchCard(m));
        }
    }

    // ─── Match Card ──────────────────────────────────────────────────

    private HBox createMatchCard(MatchEntity m) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #141A23; -fx-padding: 15 20; -fx-background-radius: 10; " +
                      "-fx-border-color: #2A3441; -fx-border-radius: 10; -fx-border-width: 1;");

        // Status badge
        String statusColor = getStatusColor(m.getStatus());
        Label statusBadge = new Label("● " + (m.getStatus() != null ? m.getStatus().name() : "SCHEDULED"));
        statusBadge.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 10px; -fx-font-weight: bold; " +
                             "-fx-background-color: " + toAlpha(statusColor, 0.15) + "; " +
                             "-fx-padding: 4 10; -fx-background-radius: 12;");
        statusBadge.setMinWidth(110);

        // Team 1
        Label team1Label = new Label(m.getTeam1() != null ? m.getTeam1().getName() : "TBD");
        team1Label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        team1Label.setMinWidth(140);
        team1Label.setAlignment(Pos.CENTER_RIGHT);

        // VS
        Label vsLabel = new Label("VS");
        vsLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px; -fx-font-weight: bold;");
        vsLabel.setMinWidth(30);
        vsLabel.setAlignment(Pos.CENTER);

        // Team 2
        Label team2Label = new Label(m.getTeam2() != null ? m.getTeam2().getName() : "TBD");
        team2Label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        team2Label.setMinWidth(140);

        // Separator
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Score (if available)
        String scoreText = m.getScore() != null ? m.getScore() : "—";
        Label scoreLabel = new Label(scoreText);
        scoreLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 60px; -fx-alignment: center;");

        // Date
        String dateStr = m.getScheduledAt() != null ? m.getScheduledAt().format(DT_FMT) : "—";
        Label dateLabel = new Label("📅 " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");

        // Winner label
        if (m.getWinner() != null) {
            Label winnerLabel = new Label("🏆 " + m.getWinner().getName());
            winnerLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 11px; -fx-font-weight: bold;");
            card.getChildren().addAll(statusBadge, team1Label, vsLabel, team2Label, spacer, scoreLabel, dateLabel, winnerLabel);
        } else {
            card.getChildren().addAll(statusBadge, team1Label, vsLabel, team2Label, spacer, scoreLabel, dateLabel);
        }
        
        // Phase 4: Drag & Drop Scheduling (Swapping Match Dates)
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(m.getId().toString()); 
            db.setContent(content);
            event.consume();
        });

        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        card.setOnDragEntered(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                card.setStyle("-fx-background-color: #1A212D; -fx-padding: 15 20; -fx-background-radius: 10; " +
                              "-fx-border-color: #E50914; -fx-border-radius: 10; -fx-border-width: 2;");
            }
            event.consume();
        });

        card.setOnDragExited(event -> {
            card.setStyle("-fx-background-color: #141A23; -fx-padding: 15 20; -fx-background-radius: 10; " +
                          "-fx-border-color: #2A3441; -fx-border-radius: 10; -fx-border-width: 1;");
            event.consume();
        });

        card.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String draggedMatchIdStr = db.getString();
                if (currentMatches != null) {
                    MatchEntity draggedMatch = currentMatches.stream()
                        .filter(match -> match.getId() != null && match.getId().toString().equals(draggedMatchIdStr))
                        .findFirst().orElse(null);
                        
                    if (draggedMatch != null && draggedMatch.getId() != m.getId()) {
                        LocalDateTime tempDate = draggedMatch.getScheduledAt();
                        draggedMatch.setScheduledAt(m.getScheduledAt());
                        m.setScheduledAt(tempDate);
                        
                        try {
                            matchService.updateMatchDate(draggedMatch.getId(), draggedMatch.getScheduledAt());
                            matchService.updateMatchDate(m.getId(), m.getScheduledAt());
                            loadExistingMatches(); // Refresh UI
                            
                            statusWarningLabel.setText("✅ Horaires des matchs échangés avec succès !");
                            statusWarningLabel.setStyle("-fx-text-fill: #4ADE80; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-color: rgba(74,222,128,0.1); -fx-background-radius: 8;");
                            statusWarningLabel.setVisible(true);
                            success = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return card;
    }

    private String getStatusColor(MatchStatus status) {
        if (status == null) return "#9CA3AF";
        return switch (status) {
            case SCHEDULED   -> "#22C55E";
            case IN_PROGRESS -> "#F59E0B";
            case COMPLETED   -> "#6B7280";
            case CANCELLED   -> "#FF4D4D";
        };
    }

    /** Fake alpha by wrapping color in rgba-like inline style string */
    private String toAlpha(String hex, double alpha) {
        // Convert #RRGGBB -> rgba(r,g,b,alpha) in JavaFX inline CSS
        // For simplicity just use the hex with reduced brightness background
        if (hex.startsWith("#") && hex.length() == 7) {
            try {
                int r = Integer.parseInt(hex.substring(1, 3), 16);
                int g = Integer.parseInt(hex.substring(3, 5), 16);
                int b = Integer.parseInt(hex.substring(5, 7), 16);
                return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, alpha);
            } catch (Exception e) {
                return "rgba(150,150,150,0.15)";
            }
        }
        return "rgba(150,150,150,0.15)";
    }

    // ─── Navigation ──────────────────────────────────────────────────

    @FXML
    public void handleBack() {
        if (mainController != null && tournoi != null) {
            TournoiEditController editController = mainController.loadViewAndGetController(
                    "/com/carthage/view/admin/tournoi-edit-view.fxml");
            if (editController != null) {
                editController.setTournamentAndMainController(tournoi.getId(), mainController);
            }
        }
    }

    @FXML
    public void handleTestEmails() {
        // Lance manuellement la vérification des matchs dans les 24h et envoie les rappels
        com.carthage.services.EmailService.testSendRemindersNow();
        new Alert(Alert.AlertType.INFORMATION, "Le test d'envoi d'e-mails a été déclenché.\nLes e-mails seront envoyés pour les matchs prévus dans moins de 24h.\nVérifiez la console et les boîtes de réception des capitaines !").showAndWait();
    }
}
