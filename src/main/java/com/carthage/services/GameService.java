package com.carthage.services;

import com.carthage.entity.Game;
import com.carthage.entity.enums.GameStatus;
import com.carthage.entity.enums.GameType;
import com.carthage.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameService {

    private final Connection connection;

    public GameService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public List<Game> getAllGames() {
        List<Game> games = new ArrayList<>();
        // Query to get game details, and count of tournaments and skins
        String sql = "SELECT HEX(g.id) as id, g.name, g.description, g.type, g.status, g.image_url, g.created_at, " +
                     "(SELECT COUNT(*) FROM tournoi t WHERE t.game_id = g.id) as tournoi_count, " +
                     "(SELECT COUNT(*) FROM skin s WHERE s.game_id = g.id) as skin_count " +
                     "FROM game g";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Game game = new Game();
                game.setId(hexToUUID(rs.getString("id")));
                game.setName(rs.getString("name"));
                game.setDescription(rs.getString("description"));
                
                String typeStr = rs.getString("type");
                try {
                    game.setType(GameType.valueOf(typeStr.toUpperCase()));
                } catch (Exception e) {
                    game.setType(GameType.FPS); // Fallback
                }

                String statusStr = rs.getString("status");
                try {
                    game.setStatus(GameStatus.valueOf(statusStr.toUpperCase()));
                } catch (Exception e) {
                    game.setStatus(GameStatus.INACTIVE); // Fallback
                }
                
                game.setImageUrl(rs.getString("image_url"));
                if (rs.getTimestamp("created_at") != null) {
                    game.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }

                // Temp storage: we use lists with dummy objects just to carry size for the UI bounds
                int tCount = rs.getInt("tournoi_count");
                List<com.carthage.entity.Tournoi> dummyTournois = new ArrayList<>();
                for(int i=0; i<tCount; i++) dummyTournois.add(new com.carthage.entity.Tournoi());
                game.setTournois(dummyTournois);

                int sCount = rs.getInt("skin_count");
                List<com.carthage.entity.Skin> dummySkins = new ArrayList<>();
                for(int i=0; i<sCount; i++) dummySkins.add(new com.carthage.entity.Skin());
                game.setSkins(dummySkins);

                games.add(game);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return games;
    }

    private static UUID hexToUUID(String hex) {
        if (hex == null || hex.length() != 32) return null;
        String withDashes = hex.substring(0, 8) + "-" +
                            hex.substring(8, 12) + "-" +
                            hex.substring(12, 16) + "-" +
                            hex.substring(16, 20) + "-" +
                            hex.substring(20);
        return UUID.fromString(withDashes);
    }

    public void create(Game game) {
        String sql = "INSERT INTO game (id, name, description, type, status, image_url, created_at) VALUES (UNHEX(?), ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, game.getId().toString().replace("-", ""));
            ps.setString(2, game.getName());
            ps.setString(3, game.getDescription());
            ps.setString(4, game.getType().name());
            ps.setString(5, game.getStatus().name());
            ps.setString(6, game.getImageUrl());
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(game.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Game game) {
        String sql = "UPDATE game SET name = ?, description = ?, type = ?, status = ?, image_url = ? WHERE id = UNHEX(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, game.getName());
            ps.setString(2, game.getDescription());
            ps.setString(3, game.getType().name());
            ps.setString(4, game.getStatus().name());
            ps.setString(5, game.getImageUrl());
            ps.setString(6, game.getId().toString().replace("-", ""));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM game WHERE id = UNHEX(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString().replace("-", ""));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
