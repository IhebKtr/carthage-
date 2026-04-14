package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.DatabaseConnection;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.*;

public class DashboardController {

    @FXML private Label activeTournoisLabel;
    @FXML private Label registeredTeamsLabel;
    @FXML private Label ongoingMatchesLabel;
    @FXML private Label supportTicketsLabel;

    private Connection connection;

    @FXML
    public void initialize() {
        connection = DatabaseConnection.getInstance().getConnection();
        loadStats();
    }

    private void loadStats() {
        User user = SessionContext.getInstance().getCurrentUser();
        
        // 1. Active Tournaments
        activeTournoisLabel.setText(String.valueOf(getCount("SELECT COUNT(*) FROM tournoi WHERE status != 'COMPLETED'")));
        
        // 2. Registered Teams
        registeredTeamsLabel.setText(String.valueOf(getCount("SELECT COUNT(*) FROM team")));
        
        // 3. Ongoing Matches
        ongoingMatchesLabel.setText(String.valueOf(getCount("SELECT COUNT(*) FROM match_game WHERE status = 'ONGOING'")));
        
        // 4. User Support Tickets
        if (user != null) {
            String sql = "SELECT COUNT(*) FROM reclamation WHERE author_id = UNHEX(REPLACE(?, '-', '')) AND status != 'CLOSED'";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, user.getId().toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) supportTicketsLabel.setText(String.valueOf(rs.getInt(1)));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private int getCount(String sql) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
