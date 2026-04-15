package com.carthagearena.service;

import com.carthagearena.model.Merch;
import com.carthagearena.model.Game;
import com.carthagearena.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Merch - couche métier
 * Gère toutes les opérations CRUD sur les produits Merch
 */
public class MerchService {

    // ─── CREATE ──────────────────────────────────────────────────────────────

    public void create(Merch merch) throws SQLException {
        String sql = """
                INSERT INTO merch (id, name, description, price, stock, image_url, type, created_at, game_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, merch.getId().toString());
            stmt.setString(2, merch.getName());
            stmt.setString(3, merch.getDescription());
            stmt.setInt(4, merch.getPrice());
            stmt.setInt(5, merch.getStock());
            stmt.setString(6, merch.getImageUrl());
            stmt.setString(7, merch.getType());
            stmt.setTimestamp(8, Timestamp.valueOf(merch.getCreatedAt()));
            if (merch.getGame() != null) {
                stmt.setInt(9, merch.getGame().getId());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }
            stmt.executeUpdate();
        }
    }

    // ─── READ ALL ─────────────────────────────────────────────────────────────

    public List<Merch> findAll() throws SQLException {
        String sql = """
                SELECT m.*, g.id as game_id, g.name as game_name
                FROM merch m
                LEFT JOIN game g ON m.game_id = g.id
                ORDER BY m.created_at DESC
                """;

        List<Merch> list = new ArrayList<>();
        try (Statement stmt = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    // ─── SEARCH ──────────────────────────────────────────────────────────────

    public List<Merch> searchByName(String keyword) throws SQLException {
        String sql = "SELECT m.*, g.id as game_id, g.name as game_name " +
                     "FROM merch m LEFT JOIN game g ON m.game_id = g.id " +
                     "WHERE m.name LIKE ? ORDER BY m.name";

        List<Merch> list = new ArrayList<>();
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Merch> findByType(String type) throws SQLException {
        String sql = "SELECT m.*, g.id as game_id, g.name as game_name " +
                     "FROM merch m LEFT JOIN game g ON m.game_id = g.id " +
                     "WHERE m.type = ? ORDER BY m.name";

        List<Merch> list = new ArrayList<>();
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, type);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    // ─── READ ONE ─────────────────────────────────────────────────────────────

    public Optional<Merch> findById(UUID id) throws SQLException {
        String sql = "SELECT m.*, g.id as game_id, g.name as game_name " +
                     "FROM merch m LEFT JOIN game g ON m.game_id = g.id WHERE m.id = ?";

        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
        }
        return Optional.empty();
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────

    public void update(Merch merch) throws SQLException {
        String sql = """
                UPDATE merch
                SET name = ?, description = ?, price = ?, stock = ?,
                    image_url = ?, type = ?, game_id = ?
                WHERE id = ?
                """;

        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, merch.getName());
            stmt.setString(2, merch.getDescription());
            stmt.setInt(3, merch.getPrice());
            stmt.setInt(4, merch.getStock());
            stmt.setString(5, merch.getImageUrl());
            stmt.setString(6, merch.getType());
            if (merch.getGame() != null) {
                stmt.setInt(7, merch.getGame().getId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            stmt.setString(8, merch.getId().toString());
            stmt.executeUpdate();
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM merch WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            stmt.executeUpdate();
        }
    }

    // ─── STOCK ───────────────────────────────────────────────────────────────

    public void decreaseStock(UUID id, int quantity) throws SQLException {
        String sql = "UPDATE merch SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, id.toString());
            stmt.setInt(3, quantity);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Stock insuffisant pour le produit : " + id);
            }
        }
    }

    // ─── MAPPING ─────────────────────────────────────────────────────────────

    private Merch mapResultSet(ResultSet rs) throws SQLException {
        Merch merch = new Merch();
        merch.setId(UUID.fromString(rs.getString("id")));
        merch.setName(rs.getString("name"));
        merch.setDescription(rs.getString("description"));
        merch.setPrice(rs.getInt("price"));
        merch.setStock(rs.getInt("stock"));
        merch.setImageUrl(rs.getString("image_url"));
        merch.setType(rs.getString("type"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) merch.setCreatedAt(ts.toLocalDateTime());

        // Game relation
        String gameId = rs.getString("game_id");
        if (gameId != null && !rs.wasNull()) {
            Game game = new Game();
            game.setId(rs.getInt("game_id"));
            game.setName(rs.getString("game_name"));
            merch.setGame(game);
        }
        return merch;
    }
}
