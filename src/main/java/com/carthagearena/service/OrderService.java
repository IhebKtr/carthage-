package com.carthagearena.service;

import com.carthagearena.model.Order;
import com.carthagearena.model.OrderItem;
import com.carthagearena.model.Merch;
import com.carthagearena.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service Order - couche métier
 * Gère le cycle de vie des commandes (création, paiement, annulation, historique)
 */
public class OrderService {

    private final MerchService merchService;

    public OrderService() {
        this.merchService = new MerchService();
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * Crée une commande et décrément le stock de chaque produit
     */
    public void createOrder(Order order) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false); // Transaction

        try {
            // 1. Insérer la commande
            String sqlOrder = """
                    INSERT INTO `order` (id, date, total_amount, status, user_id)
                    VALUES (?, ?, ?, ?, UNHEX(REPLACE(?, '-', '')))
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sqlOrder)) {
                stmt.setString(1, order.getId().toString());
                stmt.setTimestamp(2, Timestamp.valueOf(order.getDate()));
                stmt.setInt(3, order.getTotalAmount());
                stmt.setString(4, order.getStatus().name());
                stmt.setString(5, order.getUserId().toString());
                stmt.executeUpdate();
            }

            // 2. Insérer les lignes de commande
            String sqlItem = """
                    INSERT INTO order_item (order_id, merch_id, quantity, unit_price)
                    VALUES (?, ?, ?, ?)
                    """;
            for (OrderItem item : order.getItems()) {
                try (PreparedStatement stmt = conn.prepareStatement(sqlItem)) {
                    stmt.setString(1, order.getId().toString());
                    stmt.setString(2, item.getMerch().getId().toString());
                    stmt.setInt(3, item.getQuantity());
                    stmt.setInt(4, item.getUnitPrice());
                    stmt.executeUpdate();
                }

                // 3. Décrémenter le stock
                merchService.decreaseStock(item.getMerch().getId(), item.getQuantity());
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─── READ ALL ─────────────────────────────────────────────────────────────

    public List<Order> findAll() throws SQLException {
        String sql = """
                SELECT o.*, HEX(o.user_id) as user_uuid, u.username as user_name
                FROM `order` o
                LEFT JOIN user u ON o.user_id = u.id
                ORDER BY o.date DESC
                """;

        List<Order> list = new ArrayList<>();
        try (Statement stmt = DatabaseConnection.getInstance().getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        }
        return list;
    }

    public List<Order> findByUserId(UUID userId) throws SQLException {
        String sql = "SELECT o.*, HEX(o.user_id) as user_uuid, u.username as user_name FROM `order` o " +
                     "LEFT JOIN user u ON o.user_id = u.id " +
                     "WHERE o.user_id = UNHEX(REPLACE(?, '-', '')) ORDER BY o.date DESC";

        List<Order> list = new ArrayList<>();
        try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        }
        return list;
    }

    public Order findById(String id) throws SQLException {
        String sql = "SELECT o.*, HEX(o.user_id) as user_uuid, u.username as user_name FROM `order` o " +
                     "LEFT JOIN user u ON o.user_id = u.id WHERE o.id = ?";

        try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Order order = mapOrder(rs);
                order.setItems(findItemsByOrderId(id));
                return order;
            }
        }
        return null;
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────

    public void updateStatus(String orderId, Order.Status newStatus) throws SQLException {
        String sql = "UPDATE `order` SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setString(2, orderId);
            stmt.executeUpdate();
        }
    }

    public void payOrder(String orderId) throws SQLException {
        updateStatus(orderId, Order.Status.PAID);
    }

    public void cancelOrder(String orderId) throws SQLException {
        updateStatus(orderId, Order.Status.CANCELLED);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    public void delete(String orderId) throws SQLException {
        try {
            DatabaseConnection.getInstance().getConnection().setAutoCommit(false);
            // D'abord supprimer les lignes de commande
            String sqlItems = "DELETE FROM order_item WHERE order_id = ?";
            try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sqlItems)) {
                stmt.setString(1, orderId);
                stmt.executeUpdate();
            }
            // Puis supprimer la commande
            String sqlOrder = "DELETE FROM `order` WHERE id = ?";
            try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sqlOrder)) {
                stmt.setString(1, orderId);
                stmt.executeUpdate();
            }
            DatabaseConnection.getInstance().getConnection().commit();
        } catch (SQLException e) {
            DatabaseConnection.getInstance().getConnection().rollback();
            throw e;
        } finally {
            DatabaseConnection.getInstance().getConnection().setAutoCommit(true);
        }
    }

    // ─── ITEMS ───────────────────────────────────────────────────────────────

    private List<OrderItem> findItemsByOrderId(String orderId) throws SQLException {
        String sql = """
                SELECT oi.*, m.id as merch_uuid, m.name as merch_name,
                       m.price as merch_price, m.type as merch_type
                FROM order_item oi
                JOIN merch m ON oi.merch_id = m.id
                WHERE oi.order_id = ?
                """;

        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement stmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getInt("unit_price"));

                Merch merch = new Merch();
                merch.setId(UUID.fromString(rs.getString("merch_uuid")));
                merch.setName(rs.getString("merch_name"));
                merch.setPrice(rs.getInt("merch_price"));
                merch.setType(rs.getString("merch_type"));
                item.setMerch(merch);

                items.add(item);
            }
        }
        return items;
    }

    // ─── ANALYTICS ──────────────────────────────────────────────────────────

    public Map<String, Integer> getGenderStats() throws SQLException {
        String sql = "SELECT gender, COUNT(*) as count FROM user GROUP BY gender";
        Map<String, Integer> stats = new java.util.HashMap<>();
        try (Statement stmt = DatabaseConnection.getInstance().getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("gender"), rs.getInt("count"));
            }
        }
        return stats;
    }

    public Map<String, Integer> getAgeStats() throws SQLException {
        String sql = "SELECT " +
                     "  CASE " +
                     "    WHEN age < 18 THEN 'Under 18' " +
                     "    WHEN age BETWEEN 18 AND 25 THEN '18-25' " +
                     "    WHEN age BETWEEN 26 AND 35 THEN '26-35' " +
                     "    WHEN age BETWEEN 36 AND 50 THEN '36-50' " +
                     "    ELSE '50+' " +
                     "  END as age_group, COUNT(*) as count " +
                     "FROM user GROUP BY age_group";
        Map<String, Integer> stats = new java.util.HashMap<>();
        try (Statement stmt = DatabaseConnection.getInstance().getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("age_group"), rs.getInt("count"));
            }
        }
        return stats;
    }

    // ─── MAPPING ─────────────────────────────────────────────────────────────

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(UUID.fromString(rs.getString("id")));
        order.setDate(rs.getTimestamp("date").toLocalDateTime());
        order.setTotalAmount(rs.getInt("total_amount"));
        order.setStatus(Order.Status.valueOf(rs.getString("status")));
        
        String userUuid = rs.getString("user_uuid");
        if (userUuid != null) {
            // Re-formater l'hex en format UUID (8-4-4-4-12)
            String formattedUuid = userUuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
            );
            order.setUserId(UUID.fromString(formattedUuid));
        }
        
        String name = rs.getString("user_name");
        order.setUserFullName(name != null ? name : "Utilisateur #" + (userUuid != null ? userUuid.substring(0,8) : "???"));
        return order;
    }
}
