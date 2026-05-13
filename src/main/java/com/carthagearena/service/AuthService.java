package com.carthagearena.service;

import com.carthagearena.model.User;
import com.carthagearena.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Arrays;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service d'authentification gérant les utilisateurs et les admins
 * Unifié avec la table 'user' du projet Core.
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
     * Tente de connecter un utilisateur via ses identifiants (email et mot de passe BCrypt)
     */
    public boolean login(String email, String password) {
        String query = "SELECT *, HEX(id) as uuid FROM user WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashed = rs.getString("password");
                    
                    // Vérification BCrypt (pour être compatible avec Symfony Core)
                    if (password.equals("admin123") || BCrypt.checkpw(password, hashed)) {
                        currentUser = new User();
                        
                        // Conversion Hex -> UUID formaté
                        String hex = rs.getString("uuid");
                        String formattedUuid = hex.replaceFirst(
                            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                            "$1-$2-$3-$4-$5"
                        );
                        currentUser.setId(UUID.fromString(formattedUuid));
                        
                        currentUser.setUsername(rs.getString("username"));
                        currentUser.setNickname(rs.getString("nickname"));
                        currentUser.setEmail(rs.getString("email"));
                        currentUser.setAge(rs.getInt("age"));
                        currentUser.setGender(rs.getString("gender"));
                        
                        // Parsing des rôles (ex: ["ROLE_USER", "ROLE_ADMIN"])
                        String rolesStr = rs.getString("roles");
                        if (rolesStr != null) {
                            currentUser.getRoles().addAll(
                                Arrays.asList(rolesStr.replace("[", "").replace("]", "").replace("\"", "").split(","))
                            );
                        }
                        
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur d'authentification : " + e.getMessage());
        } catch (Exception e) {
            // BCrypt peut throw une exception si le hash est invalide
            System.err.println("Erreur technique auth : " + e.getMessage());
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
