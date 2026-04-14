package com.carthage.services;

import com.carthage.entity.Reclamation;
import com.carthage.entity.ReclamationResponse;
import com.carthage.entity.User;
import com.carthage.entity.enums.ReclamationCategory;
import com.carthage.entity.enums.ReclamationPriority;
import com.carthage.entity.enums.ReclamationStatus;
import com.carthage.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReclamationService {

    private final Connection connection;

    public ReclamationService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public List<Reclamation> getAllReclamations() {
        List<Reclamation> list = new ArrayList<>();
        String sql = "SELECT HEX(r.id) as id, r.subject, r.message, r.category, r.priority, r.status, r.created_at, " +
                     "u.email, u.username " +
                     "FROM reclamation r " +
                     "LEFT JOIN user u ON r.author_id = u.id " +
                     "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reclamation r = mapResultSetToReclamation(rs);
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateStatus(UUID id, ReclamationStatus status) throws SQLException {
        String sql = "UPDATE reclamation SET status = ?, updated_at = NOW() WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, id.toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID id) throws SQLException {
        // First delete responses
        String sqlDelResp = "DELETE FROM reclamation_response WHERE reclamation_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sqlDelResp)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        }
        
        String sqlDelRec = "DELETE FROM reclamation WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sqlDelRec)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        }
    }

    public void addResponse(ReclamationResponse response) throws SQLException {
        String sql = "INSERT INTO reclamation_response (id, message, is_admin_response, created_at, reclamation_id, author_id) " +
                     "VALUES (UNHEX(REPLACE(UUID(), '-', '')), ?, ?, NOW(), UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, response.getMessage());
            ps.setBoolean(2, response.isIsAdminResponse());
            ps.setString(3, response.getReclamation().getId().toString());
            ps.setString(4, response.getAuthor().getId().toString());
            ps.executeUpdate();
        }
    }

    public List<ReclamationResponse> getResponses(UUID reclamationId) {
        List<ReclamationResponse> list = new ArrayList<>();
        String sql = "SELECT HEX(rr.id) as id, rr.message, rr.is_admin_response, rr.created_at, " +
                     "u.username " +
                     "FROM reclamation_response rr " +
                     "LEFT JOIN user u ON rr.author_id = u.id " +
                     "WHERE rr.reclamation_id = UNHEX(REPLACE(?, '-', '')) " +
                     "ORDER BY rr.created_at ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reclamationId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ReclamationResponse rr = new ReclamationResponse();
                rr.setId(hexToUUID(rs.getString("id")));
                rr.setMessage(rs.getString("message"));
                rr.setIsAdminResponse(rs.getBoolean("is_admin_response"));
                if (rs.getTimestamp("created_at") != null) {
                    rr.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
                User author = new User();
                author.setUsername(rs.getString("username"));
                rr.setAuthor(author);
                list.add(rr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Reclamation getById(UUID id) {
        String sql = "SELECT HEX(r.id) as id, r.subject, r.message, r.category, r.priority, r.status, r.created_at, " +
                     "u.email, u.username " +
                     "FROM reclamation r " +
                     "LEFT JOIN user u ON r.author_id = u.id " +
                     "WHERE r.id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToReclamation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Reclamation mapResultSetToReclamation(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setId(hexToUUID(rs.getString("id")));
        r.setSubject(rs.getString("subject"));
        r.setMessage(rs.getString("message"));
        
        try { r.setCategory(ReclamationCategory.valueOf(rs.getString("category").toUpperCase())); }
        catch (Exception e) {}
        
        try { r.setPriority(ReclamationPriority.valueOf(rs.getString("priority").toUpperCase())); }
        catch (Exception e) { r.setPriority(ReclamationPriority.MEDIUM); }
        
        try { r.setStatus(ReclamationStatus.valueOf(rs.getString("status").toUpperCase())); }
        catch (Exception e) { r.setStatus(ReclamationStatus.PENDING); }

        if (rs.getTimestamp("created_at") != null) {
            r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }

        User author = new User();
        author.setEmail(rs.getString("email"));
        author.setUsername(rs.getString("username"));
        r.setAuthor(author);
        return r;
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
}
