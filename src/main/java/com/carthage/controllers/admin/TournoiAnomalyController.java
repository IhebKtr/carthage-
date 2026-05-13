package com.carthage.controllers.admin;

import com.carthage.entity.MatchEntity;
import com.carthage.entity.Tournoi;
import com.carthage.services.AnomalyDetectionService;
import com.carthage.services.AnomalyDetectionService.Anomaly;
import com.carthage.services.AnomalyDetectionService.Severity;
import com.carthage.services.MatchService;
import com.carthage.services.TournoiService;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

public class TournoiAnomalyController {

    // ─── FXML Nodes ─────────────────────────────────────────────────────────
    @FXML private VBox  anomalyListContainer;
    @FXML private Label statTotal;
    @FXML private Label statHigh;
    @FXML private Label statMedium;
    @FXML private Label statLow;
    @FXML private Label statTournaments;
    @FXML private Label statMatches;
    @FXML private Button refreshBtn;
    @FXML private Label  statusLabel;

    // ─── Services ────────────────────────────────────────────────────────────
    private final TournoiService           tournoiService  = new TournoiService();
    private final MatchService             matchService    = new MatchService();
    private final AnomalyDetectionService  anomalyService  = new AnomalyDetectionService();

    @FXML
    public void initialize() {
        runAnalysis();
    }

    // ─── Core Analysis ───────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() {
        runAnalysis();
    }

    /**
     * Result object that carries everything computed off the FX thread.
     */
    private record AnalysisResult(
        List<Anomaly>      anomalies,
        Map<String,Integer> stats
    ) {}

    /**
     * Runs all DB and computation work on a daemon background thread via
     * {@link Task}, then marshals the results back to the FX thread.
     * This keeps the UI responsive and lets "Analysing…" actually render.
     */
    private void runAnalysis() {
        // ── Immediate FX-thread UI reset ────────────────────────────────────
        refreshBtn.setDisable(true);
        statusLabel.setText("⏳ Analysing…");
        statusLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px;");
        anomalyListContainer.getChildren().clear();
        resetStatCards();

        // ── Background Task ─────────────────────────────────────────────────
        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() {
                // 1. Load all tournaments
                List<Tournoi> tournois = tournoiService.getAllTournois();

                // 2. Count total matches (one query per tournament)
                int totalMatches = 0;
                for (Tournoi t : tournois) {
                    List<MatchEntity> m = matchService.getMatchesByTournament(t.getId());
                    totalMatches += m.size();
                }

                // 3. Run anomaly detectors
                List<Anomaly> anomalies = anomalyService.detectAll(tournois);
                Map<String, Integer> stats = anomalyService.quickStats(
                        anomalies, tournois.size(), totalMatches);

                return new AnalysisResult(anomalies, stats);
            }
        };

        // ── On success: update UI on FX thread ──────────────────────────────
        task.setOnSucceeded(evt -> {
            AnalysisResult result = task.getValue();

            // Update stat cards
            statTotal.setText(String.valueOf(result.stats().get("total_anomalies")));
            statHigh.setText(String.valueOf(result.stats().get("high_count")));
            statMedium.setText(String.valueOf(result.stats().get("medium_count")));
            statLow.setText(String.valueOf(result.stats().get("low_count")));
            statTournaments.setText(String.valueOf(result.stats().get("tournaments_analysed")));
            statMatches.setText(String.valueOf(result.stats().get("matches_analysed")));

            // Render anomaly cards
            anomalyListContainer.getChildren().clear();
            if (result.anomalies().isEmpty()) {
                renderEmptyState();
            } else {
                for (Anomaly a : result.anomalies()) {
                    anomalyListContainer.getChildren().add(createAnomalyCard(a));
                }
            }

            int total = result.stats().get("total_anomalies");
            statusLabel.setText("✔ Analysis complete — " + total + " anomaly(ies) found across "
                    + result.stats().get("tournaments_analysed") + " tournaments.");
            statusLabel.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 12px;");
            refreshBtn.setDisable(false);
        });

        // ── On failure: show error ───────────────────────────────────────────
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            statusLabel.setText("❌ Analysis failed: " + (ex != null ? ex.getMessage() : "unknown error"));
            statusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
            refreshBtn.setDisable(false);
            if (ex != null) ex.printStackTrace();
        });

        // ── Launch daemon thread ─────────────────────────────────────────────
        Thread thread = new Thread(task);
        thread.setDaemon(true);   // won't block JVM shutdown
        thread.start();
    }

    /** Reset all stat labels to "—" while loading. */
    private void resetStatCards() {
        statTotal.setText("—");
        statHigh.setText("—");
        statMedium.setText("—");
        statLow.setText("—");
        statTournaments.setText("—");
        statMatches.setText("—");
    }

    // ─── Card Builder ────────────────────────────────────────────────────────

    private VBox createAnomalyCard(Anomaly a) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(buildCardStyle(a.severity));

        // Row 1: severity badge + category + tournament tag
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label severityBadge = new Label(severityEmoji(a.severity) + " " + a.severity.name());
        severityBadge.setStyle(buildBadgeStyle(a.severity));

        Label categoryLabel = new Label(a.category);
        categoryLabel.setStyle("-fx-text-fill: #D1D5DB; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tournamentTag = new Label("🏟 " + a.tournamentName);
        tournamentTag.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07); " +
            "-fx-text-fill: #9CA3AF; " +
            "-fx-padding: 2 8; " +
            "-fx-background-radius: 10; " +
            "-fx-font-size: 11px;"
        );

        topRow.getChildren().addAll(severityBadge, categoryLabel, spacer, tournamentTag);

        // Row 2: title
        Label titleLabel = new Label(a.title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        // Row 3: description
        Label descLabel = new Label(a.description);
        descLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(topRow, titleLabel, descLabel);
        return card;
    }

    private String buildCardStyle(Severity sev) {
        String borderColor = switch (sev) {
            case HIGH   -> "#EF4444";
            case MEDIUM -> "#F59E0B";
            case LOW    -> "#3B82F6";
        };
        String bgColor = switch (sev) {
            case HIGH   -> "rgba(239,68,68,0.08)";
            case MEDIUM -> "rgba(245,158,11,0.08)";
            case LOW    -> "rgba(59,130,246,0.08)";
        };
        return "-fx-background-color: " + bgColor + "; " +
               "-fx-border-color: " + borderColor + "; " +
               "-fx-border-width: 0 0 0 3; " +
               "-fx-border-radius: 0 8 8 0; " +
               "-fx-background-radius: 0 8 8 0;";
    }

    private String buildBadgeStyle(Severity sev) {
        String color = switch (sev) {
            case HIGH   -> "#EF4444";
            case MEDIUM -> "#F59E0B";
            case LOW    -> "#3B82F6";
        };
        String bg = switch (sev) {
            case HIGH   -> "rgba(239,68,68,0.20)";
            case MEDIUM -> "rgba(245,158,11,0.20)";
            case LOW    -> "rgba(59,130,246,0.20)";
        };
        return "-fx-background-color: " + bg + "; " +
               "-fx-text-fill: " + color + "; " +
               "-fx-padding: 2 8; " +
               "-fx-background-radius: 10; " +
               "-fx-font-weight: bold; " +
               "-fx-font-size: 10px;";
    }

    private String severityEmoji(Severity sev) {
        return switch (sev) {
            case HIGH   -> "🔴";
            case MEDIUM -> "🟡";
            case LOW    -> "🔵";
        };
    }

    // ─── Empty State ──────────────────────────────────────────────────────────

    private void renderEmptyState() {
        VBox empty = new VBox(12);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(60, 20, 60, 20));

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("No anomalies detected");
        title.setStyle("-fx-text-fill: #22C55E; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label sub = new Label("All tournaments and matches look healthy. Keep up the good work!");
        sub.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
        sub.setWrapText(true);

        empty.getChildren().addAll(icon, title, sub);
        anomalyListContainer.getChildren().add(empty);
    }
}
