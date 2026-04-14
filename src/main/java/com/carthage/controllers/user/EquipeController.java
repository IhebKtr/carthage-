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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.*;
import java.util.UUID;

public class EquipeController {

    // --- Container ---
    @FXML
    private StackPane contentContainer;

    // --- Dashboard Fields (Loaded from equipe-dashboard-view.fxml) ---
    @FXML
    private Label teamNameLabel, teamTagLabel, levelLabel, teamDescLabel;
    @FXML
    private Label memberCountLabel, leaderLabel, winsLabel, rosterCountLabel;
    @FXML
    private Label inviteCodeLabel, winRateLabel;
    @FXML
    private VBox rosterList, inscriptionsList;

    // --- Create Form Fields (Loaded from equipe-create-view.fxml) ---
    @FXML
    private TextField createTeamName;
    @FXML
    private TextField createTeamTag;
    @FXML
    private TextArea createTeamDesc;

    // --- Join Form Fields (Loaded from equipe-join-view.fxml) ---
    @FXML
    private TextField joinTeamCode;

    private Connection connection;
    private String teamHexId = null;
    private boolean isInitialized = false;

    @FXML
    public void initialize() {
        if (isInitialized)
            return;

        connection = DatabaseConnection.getInstance().getConnection();
        User user = SessionContext.getInstance().getCurrentUser();

        if (contentContainer != null) {
            isInitialized = true;
            // This is the container root, check team status
            if (user != null && hasTeam(user)) {
                showDashboard();
            } else {
                showEmptyState();
            }
        }
    }

    private boolean hasTeam(User user) {
        String sql = "SELECT 1 FROM team_membership WHERE player_id = UNHEX(REPLACE(?, '-', '')) LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // --- View Swapping ---

    @FXML
    private void showEmptyState() {
        loadSubView("/com/carthage/view/user/equipe-empty-view.fxml");
    }

    @FXML
    private void showCreateForm() {
        loadSubView("/com/carthage/view/user/equipe-create-view.fxml");
    }

    @FXML
    private void showJoinForm() {
        loadSubView("/com/carthage/view/user/equipe-join-view.fxml");
    }

    @FXML
    private void showDashboard() {
        loadSubView("/com/carthage/view/user/equipe-dashboard-view.fxml");
        User user = SessionContext.getInstance().getCurrentUser();
        if (user != null)
            loadTeam(user);
    }

    private void loadSubView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setController(this); // Use this controller instance for the subview
            Node node = loader.load();
            contentContainer.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Dashboard Logic ---

    private void loadTeam(User user) {
        String sql = "SELECT HEX(t.id) as hex_id, t.name, t.tag, t.description, t.invite_code, " +
                "  (SELECT COUNT(*) FROM team_membership tm2 WHERE tm2.team_id = t.id) as member_count " +
                "FROM team t " +
                "JOIN team_membership tm ON t.id = tm.team_id " +
                "WHERE tm.player_id = UNHEX(REPLACE(?, '-', '')) LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                teamHexId = rs.getString("hex_id");
                if (teamNameLabel != null)
                    teamNameLabel.setText(nvl(rs.getString("name"), "—"));
                if (teamTagLabel != null) {
                    String tag = rs.getString("tag");
                    teamTagLabel.setText(tag != null ? "[" + tag + "]" : "");
                }
                if (teamDescLabel != null)
                    teamDescLabel.setText(nvl(rs.getString("description"), ""));

                int members = rs.getInt("member_count");
                if (memberCountLabel != null)
                    memberCountLabel.setText("👥 " + members + " Membres");
                if (inviteCodeLabel != null)
                    inviteCodeLabel.setText(nvl(rs.getString("invite_code"), "N/A"));
                if (rosterCountLabel != null)
                    rosterCountLabel.setText(members + "/8");

                loadRoster();
                loadWins();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRoster() {
        if (rosterList == null || teamHexId == null)
            return;
        rosterList.getChildren().clear();
        String sql = "SELECT u.username, tm.role FROM team_membership tm " +
                "JOIN user u ON u.id = tm.player_id " +
                "WHERE tm.team_id = UNHEX(?) ORDER BY tm.role DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamHexId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                if ("CAPTAIN".equalsIgnoreCase(role) && leaderLabel != null)
                    leaderLabel.setText("👑 Leader " + username);
                rosterList.getChildren().add(buildRosterRow(username, role));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadWins() {
        if (winsLabel == null || teamHexId == null)
            return;
        String sql = "SELECT COUNT(*) as wins FROM match_game WHERE winner_id = UNHEX(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, teamHexId);
            ResultSet rs = ps.executeQuery();
            winsLabel.setText(rs.next() ? String.valueOf(rs.getInt("wins")) : "0");
        } catch (SQLException e) {
            winsLabel.setText("0");
        }
    }

    // --- Action Handlers ---

    @FXML
    private void onCreateTeamSubmit() {
        String name = createTeamName.getText();
        String tag = createTeamTag.getText();
        String desc = createTeamDesc.getText();

        if (name.isEmpty() || tag.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir les champs obligatoires (*)");
            return;
        }

        User user = SessionContext.getInstance().getCurrentUser();
        String teamId = UUID.randomUUID().toString().replace("-", "");
        String inviteCode = (tag + "-" + UUID.randomUUID().toString().substring(0, 4)).toUpperCase();

        try {
            connection.setAutoCommit(false);
            // 1. Insert Team
            String sqlTeam = "INSERT INTO team (id, name, tag, description, invite_code, status, created_at, captain_id) "
                    +
                    "VALUES (UNHEX(?), ?, ?, ?, ?, 'ACTIVE', NOW(), UNHEX(REPLACE(?, '-', '')))";
            try (PreparedStatement ps = connection.prepareStatement(sqlTeam)) {
                ps.setString(1, teamId);
                ps.setString(2, name);
                ps.setString(3, tag);
                ps.setString(4, desc);
                ps.setString(5, inviteCode);
                ps.setString(6, user.getId().toString());
                ps.executeUpdate();
            }

            // 2. Insert Membership (Leader)
            String sqlMem = "INSERT INTO team_membership (id, team_id, player_id, role, joined_at) VALUES (UNHEX(?), UNHEX(?), UNHEX(REPLACE(?, '-', '')), 'CAPTAIN', NOW())";
            try (PreparedStatement ps = connection.prepareStatement(sqlMem)) {
                ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
                ps.setString(2, teamId);
                ps.setString(3, user.getId().toString());
                ps.executeUpdate();
            }

            connection.commit();
            showDashboard();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
            }
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer l'équipe: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
            }
        }
    }

    @FXML
    private void onJoinTeamSubmit() {
        String code = joinTeamCode.getText();
        if (code.isEmpty())
            return;

        User user = SessionContext.getInstance().getCurrentUser();
        String sqlFind = "SELECT id FROM team WHERE invite_code = ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sqlFind)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                byte[] teamId = rs.getBytes("id");
                String sqlJoin = "INSERT INTO team_membership (id, team_id, player_id, role, joined_at) VALUES (UNHEX(?), ?, UNHEX(REPLACE(?, '-', '')), 'PLAYER', NOW())";
                try (PreparedStatement ps2 = connection.prepareStatement(sqlJoin)) {
                    ps2.setString(1, UUID.randomUUID().toString().replace("-", ""));
                    ps2.setBytes(2, teamId);
                    ps2.setString(3, user.getId().toString());
                    ps2.executeUpdate();
                }
                showDashboard();
            } else {
                showAlert("Erreur", "Code d'invitation invalide.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de rejoindre l'équipe.");
        }
    }

    @FXML
    public void onCopyCode() {
        String code = inviteCodeLabel.getText();
        if (code == null || code.equals("XXXXXXX"))
            return;
        ClipboardContent cc = new ClipboardContent();
        cc.putString(code);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private HBox buildRosterRow(String username, String role) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.setStyle("-fx-background-color: #0B0E14; -fx-background-radius: 8px;");
        String initial = (username != null && !username.isEmpty()) ? String.valueOf(username.charAt(0)).toUpperCase()
                : "?";
        Label avatar = new Label(initial);
        avatar.setMinWidth(36);
        avatar.setMinHeight(36);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: #1E2633; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18px;");
        Label name = new Label(username != null ? username : "—");
        name.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        boolean isCapt = "CAPTAIN".equalsIgnoreCase(role);
        Label badge = new Label(isCapt ? "LEADER" : role != null ? role.toUpperCase() : "MEMBRE");
        badge.setStyle("-fx-background-color: " + (isCapt ? "#F59E0B" : "#2A3441") + "; -fx-text-fill: "
                + (isCapt ? "#0B0E14" : "#9CA3AF")
                + "; -fx-background-radius: 4px; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        row.getChildren().addAll(avatar, name, spacer, badge);
        return row;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    public void onBrowseTournois() {
    }

    @FXML
    public void onEdit() {
    }

    @FXML
    public void onInvitations() {
    }

    @FXML
    public void onDissolve() {
    }

    private String nvl(String s, String def) {
        return s != null ? s : def;
    }
}
