package com.carthage.services;

import com.carthage.entity.Game;
import com.carthage.entity.Skin;
import com.carthage.entity.enums.SkinRarity;
import com.carthage.utils.DatabaseConnection;
import com.carthage.utils.UUIDUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkinService {

    private final Connection connection;

    public SkinService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public List<Skin> getAllSkins() {
        List<Skin> skins = new ArrayList<>();
        String sql = "SELECT s.id, s.name, s.description, s.image_url, s.price, s.rarity, s.created_at, g.id as game_id, g.name as game_name " +
                     "FROM skin s LEFT JOIN game g ON s.game_id = g.id";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Skin skin = new Skin();
                skin.setId(UUIDUtils.fromBytes(rs.getBytes("id")));
                skin.setName(rs.getString("name"));
                skin.setDescription(rs.getString("description"));
                skin.setImageUrl(rs.getString("image_url"));
                skin.setPrice(rs.getInt("price"));
                
                String rarityStr = rs.getString("rarity");
                try {
                    skin.setRarity(SkinRarity.valueOf(rarityStr.toUpperCase()));
                } catch (Exception e) {
                    skin.setRarity(SkinRarity.COMMON);
                }

                if (rs.getTimestamp("created_at") != null) {
                    skin.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }

                if (rs.getBytes("game_id") != null) {
                    Game game = new Game();
                    game.setId(UUIDUtils.fromBytes(rs.getBytes("game_id")));
                    game.setName(rs.getString("game_name"));
                    skin.setGame(game);
                }

                skins.add(skin);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return skins;
    }

    public void create(Skin skin) {
        String sql = "INSERT INTO skin (id, name, description, image_url, price, rarity, game_id, type, created_at) " +
                     "VALUES (UNHEX(?), ?, ?, ?, ?, ?, UNHEX(?), ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUIDUtils.toCleanString(skin.getId()));
            ps.setString(2, skin.getName());
            ps.setString(3, skin.getDescription() != null ? skin.getDescription() : "");
            ps.setString(4, skin.getImageUrl());
            ps.setInt(5, skin.getPrice());
            ps.setString(6, skin.getRarity() != null ? skin.getRarity().name() : SkinRarity.COMMON.name());
            
            if (skin.getGame() != null && skin.getGame().getId() != null) {
                ps.setString(7, UUIDUtils.toCleanString(skin.getGame().getId()));
            } else {
                ps.setNull(7, java.sql.Types.VARCHAR);
            }
            ps.setString(8, "DIGITAL");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Skin skin) {
        String sql = "UPDATE skin SET name=?, description=?, image_url=?, price=?, rarity=?, game_id=UNHEX(?) " +
                     "WHERE id=UNHEX(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, skin.getName());
            ps.setString(2, skin.getDescription() != null ? skin.getDescription() : "");
            ps.setString(3, skin.getImageUrl());
            ps.setInt(4, skin.getPrice());
            ps.setString(5, skin.getRarity() != null ? skin.getRarity().name() : SkinRarity.COMMON.name());
            
            if (skin.getGame() != null && skin.getGame().getId() != null) {
                ps.setString(6, UUIDUtils.toCleanString(skin.getGame().getId()));
            } else {
                ps.setNull(6, java.sql.Types.VARCHAR);
            }
            ps.setString(7, UUIDUtils.toCleanString(skin.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM skin WHERE id=UNHEX(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUIDUtils.toCleanString(id));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
