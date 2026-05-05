package com.carthagearena.service;

import com.carthagearena.model.User;
import com.carthagearena.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service d'authentification gérant les utilisateurs et les admins
 */
public class AuthService {
    private static AuthService instance;
    private User currentUser = null;

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Tente de connecter un utilisateur via ses identifiants (email et mot de passe)
     */
    public boolean login(String email, String password) {
        String query = "SELECT * FROM utilisateur WHERE email = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getInstance();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, password); // Note: En production, on comparerait des hash
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentUser = new User();
                    currentUser.setId(rs.getInt("id"));
                    currentUser.setNom(rs.getString("nom"));
                    currentUser.setPrenom(rs.getString("prenom"));
                    currentUser.setEmail(rs.getString("email"));
                    currentUser.setRole(rs.getString("role"));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur d'authentification : " + e.getMessage());
        }
        return false;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
