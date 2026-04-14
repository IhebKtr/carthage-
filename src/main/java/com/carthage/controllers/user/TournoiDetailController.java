package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.DatabaseConnection;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TournoiDetailController {

    @FXML private Label tournoiName, tournoiDesc, statusBadge, gameBadge;
    @FXML private Label prizeLabel, formatLabel, teamsLimitLabel, datesLabel, locationLabel;
    @FXML private Label regCountLabel, regMaxLabel, closureDateLabel;
    @FXML private ProgressBar regProgress;
    @FXML private VBox matchList, teamsList;
    @FXML private Button joinBtn;

    private Connection connection;
    private UUID currentTournoiId;
    private int maxTeams = 8;
    private static final DateTimeFormatter FMT_SHORT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        connection = DatabaseConnection.getInstance().getConnection();
    }

    public void setTournoi(UUID tournoiId) {
        this.currentTournoiId = tournoiId;
        loadTournoiData();
        loadMatches();
        loadRegisteredTeams();
        checkButtons();
    }

    private void loadTournoiData() {
        String sql = "SELECT t.*, g.name as game_name FROM tournoi t " +
                     "JOIN game g ON t.game_id = g.id " +
                     "WHERE t.id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, currentTournoiId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tournoiName.setText(rs.getString("nom"));
                tournoiDesc.setText(rs.getString("nom") + " sur " + rs.getString("game_name"));

                // Status badge with color coding
                String status = nvl(rs.getString("status"), "UPCOMING").toUpperCase();
                statusBadge.setText(status);
                switch (status) {
                    case "ONGOING" -> statusBadge.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: black; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4;");
                    case "COMPLETED" -> statusBadge.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4;");
                    default -> statusBadge.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4;");
                }

                gameBadge.setText(nvl(rs.getString("game_name"), "GAME").toUpperCase());
                prizeLabel.setText(String.valueOf(rs.getInt("prize_pool")));
                formatLabel.setText(nvl(rs.getString("type"), "Elimination"));

                maxTeams = rs.getInt("nb_equipes_max");
                teamsLimitLabel.setText(maxTeams + " équipes max.");
                regMaxLabel.setText("/ " + maxTeams);

                locationLabel.setText(nvl(rs.getString("place"), "En ligne"));
                
                Timestamp start = rs.getTimestamp("date_debut");
                Timestamp end = rs.getTimestamp("date_fin");
                if (start != null && end != null) {
                    datesLabel.setText("Du " + start.toLocalDateTime().format(FMT_SHORT) + 
                                     " au " + end.toLocalDateTime().format(FMT_SHORT));
                    closureDateLabel.setText(start.toLocalDateTime().format(FMT_SHORT));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMatches() {
        matchList.getChildren().clear();
        String sql = "SELECT m.*, t1.name as team1_name, t2.name as team2_name FROM match_game m " +
                     "LEFT JOIN team t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN team t2 ON m.team2_id = t2.id " +
                     "WHERE m.tournoi_id = UNHEX(REPLACE(?, '-', '')) " +
                     "ORDER BY m.round ASC, m.scheduled_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, currentTournoiId.toString());
            ResultSet rs = ps.executeQuery();
            
            int currentRound = -1;
            VBox roundBox = null;
            boolean hasMatches = false;

            while (rs.next()) {
                hasMatches = true;
                int round = rs.getInt("round");
                if (round != currentRound) {
                    currentRound = round;
                    String roundLabel = switch (round) {
                        case 1 -> "DEMI-FINALES";
                        case 2 -> "FINALE";
                        default -> "ROUND " + round;
                    };
                    Label roundTitle = new Label("🎯  Round " + round + "  —  " + roundLabel);
                    roundTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    roundBox = new VBox(10);
                    roundBox.setPadding(new Insets(10, 0, 20, 0));
                    matchList.getChildren().addAll(roundTitle, roundBox);
                }
                
                if (roundBox != null) {
                    roundBox.getChildren().add(buildMatchRow(
                        rs.getString("team1_name"), 
                        rs.getString("team2_name"), 
                        rs.getString("status"),
                        rs.getTimestamp("scheduled_at")
                    ));
                }
            }

            if (!hasMatches) {
                Label empty = new Label("Aucun match programmé pour l'instant.");
                empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-padding: 10 0;");
                matchList.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRegisteredTeams() {
        teamsList.getChildren().clear();
        String sql = "SELECT t.name, t.tag FROM team t " +
                     "JOIN tournoi_team tt ON t.id = tt.team_id " +
                     "WHERE tt.tournoi_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, currentTournoiId.toString());
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                teamsList.getChildren().add(buildTeamRow(rs.getString("name"), rs.getString("tag")));
                count++;
            }
            regCountLabel.setText(String.valueOf(count));
            regProgress.setProgress(maxTeams > 0 ? (double) count / maxTeams : 0);

            if (count == 0) {
                Label empty = new Label("Aucune équipe inscrite pour le moment.");
                empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-padding: 10 0;");
                teamsList.getChildren().add(empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox buildMatchRow(String t1, String t2, String status, Timestamp date) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-background-color: #141A23; -fx-background-radius: 12;");

        Label team1 = new Label(nvl(t1, "À déterminer"));
        team1.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        Label vs = new Label("VS");
        vs.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        
        Label team2 = new Label(nvl(t2, "À déterminer"));
        team2.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String st = nvl(status, "PROGRAMMÉ").toUpperCase();
        Label statusLabel = new Label(st);
        String badgeColor = switch (st) {
            case "ONGOING", "EN_COURS" -> "#F59E0B";
            case "COMPLETED", "TERMINÉ" -> "#4ADE80";
            default -> "#3B82F6";
        };
        statusLabel.setStyle("-fx-background-color: #1E2633; -fx-text-fill: " + badgeColor + "; -fx-padding: 4 8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4;");

        VBox info = new VBox(2);
        HBox teamsRow = new HBox(10, team1, vs, team2);
        teamsRow.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(date != null ? date.toLocalDateTime().format(FMT_SHORT) : "--");
        dateLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        info.getChildren().addAll(teamsRow, dateLabel);

        row.getChildren().addAll(info, spacer, statusLabel);
        return row;
    }

    private HBox buildTeamRow(String name, String tag) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-color: #141A23; -fx-background-radius: 8;");
        
        Label avatar = new Label(tag != null ? tag.substring(0, Math.min(2, tag.length())).toUpperCase() : "??");
        avatar.setStyle("-fx-background-color: #1E2633; -fx-text-fill: white; -fx-padding: 8; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        VBox info = new VBox(2);
        Label n = new Label(name); n.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label t = new Label("Inscrit ✔"); t.setStyle("-fx-text-fill: #4ADE80; -fx-font-size: 10px;");
        info.getChildren().addAll(n, t);
        
        row.getChildren().addAll(avatar, info);
        return row;
    }

    private void checkButtons() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) {
            joinBtn.setDisable(true);
            joinBtn.setText("Connectez-vous pour rejoindre");
            return;
        }

        // Check if user's team is already registered in this tournament
        String sql = "SELECT COUNT(*) AS cnt FROM tournoi_team tt " +
                     "JOIN team_membership tm ON tt.team_id = tm.team_id " +
                     "WHERE tt.tournoi_id = UNHEX(REPLACE(?, '-', '')) " +
                     "AND tm.player_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, currentTournoiId.toString());
            ps.setString(2, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("cnt") > 0) {
                joinBtn.setDisable(true);
                joinBtn.setText("✔ Déjà inscrit");
                joinBtn.setStyle("-fx-background-color: #1E2633; -fx-text-fill: #4ADE80; -fx-padding: 14 0; -fx-font-size: 16px; -fx-background-radius: 12; -fx-border-color: #4ADE80; -fx-border-radius: 12; -fx-border-width: 1;");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Check if user is a captain of any team
        String captainSql = "SELECT COUNT(*) AS cnt FROM team_membership WHERE player_id = UNHEX(REPLACE(?, '-', '')) AND role = 'CAPTAIN'";
        try (PreparedStatement ps = connection.prepareStatement(captainSql)) {
            ps.setString(1, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("cnt") == 0) {
                joinBtn.setDisable(true);
                joinBtn.setText("Vous devez être Leader d'une équipe");
                joinBtn.setStyle("-fx-background-color: #1E2633; -fx-text-fill: #6b7280; -fx-padding: 14 0; -fx-font-size: 14px; -fx-background-radius: 12;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML public void onBack() {
        try {
            javafx.scene.Node ca = tournoiName.getScene().lookup("#contentArea");
            if (ca != null && ca.getUserData() instanceof MainLayoutController mlc) {
                mlc.loadView("tournois-view.fxml");
            } else {
                System.err.println("MainLayoutController could not be found to navigate back.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void onJoinTournament() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) return;

        // Finding user's team where they are Captain
        String findTeam = "SELECT HEX(team_id) as tid FROM team_membership WHERE player_id = UNHEX(REPLACE(?, '-', '')) AND role = 'CAPTAIN' LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(findTeam)) {
            ps.setString(1, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String teamHexId = rs.getString("tid");
                String insert = "INSERT IGNORE INTO tournoi_team (tournoi_id, team_id) VALUES (UNHEX(REPLACE(?, '-', '')), UNHEX(?))";
                try (PreparedStatement psIns = connection.prepareStatement(insert)) {
                    psIns.setString(1, currentTournoiId.toString());
                    psIns.setString(2, teamHexId);
                    psIns.executeUpdate();
                    loadRegisteredTeams();
                    checkButtons();
                    showAlert("Succès", "Votre équipe a été inscrite avec succès !");
                }
            } else {
                showAlert("Erreur", "Seul le Leader d'une équipe peut effectuer une inscription.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    private String nvl(String s, String d) { return s != null ? s : d; }
}
