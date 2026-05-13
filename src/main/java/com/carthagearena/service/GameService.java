package com.carthagearena.service;

import com.carthagearena.model.Game;
import com.carthagearena.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GameService {

    public List<Game> findAll() throws SQLException {
        List<Game> games = new ArrayList<>();
        String sql = "SELECT * FROM game ORDER BY name ASC";

        try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Game g = new Game();
                g.setId(rs.getInt("id"));
                g.setName(rs.getString("name"));
                g.setDescription(rs.getString("description"));
                g.setLogoUrl(rs.getString("logo_url"));
                games.add(g);
            }
        }
        return games;
    }
}
