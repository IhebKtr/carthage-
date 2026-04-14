package com.carthage.services;

import com.carthage.entity.User;
import com.carthage.entity.enums.AccountStatus;
import com.carthage.utils.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * Handles user authentication and registration against the MySQL database.
 * Passwords are stored/compared as plain text for now (no hashing configured on DB side).
 */
public class UserService {

    private final Connection connection;

    public UserService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    /**
     * Tries to find a user by email and verify the password.
     *
     * @return the User if credentials match, null otherwise
     * @throws AuthException with a human-readable message on failure
     */
    public User login(String email, String plainPassword) throws AuthException {
        if (email == null || email.isBlank()) throw new AuthException("Veuillez entrer votre email.");
        if (plainPassword == null || plainPassword.isBlank()) throw new AuthException("Veuillez entrer votre mot de passe.");

        String sql = "SELECT HEX(id) as id, email, username, nickname, password, roles, balance, status, is_verified, discord_id, created_at " +
                     "FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                throw new AuthException("Aucun compte trouvé avec cet email.");
            }

            String storedPassword = rs.getString("password");
            // Symfony hashes with $2y$ prefix; jBCrypt only understands $2a$
            // They are functionally identical — just normalize the prefix before checking
            String normalizedHash = storedPassword.startsWith("$2y$")
                    ? "$2a$" + storedPassword.substring(4)
                    : storedPassword;
            if (!BCrypt.checkpw(plainPassword, normalizedHash)) {
                throw new AuthException("Mot de passe incorrect.");
            }

            String statusStr = rs.getString("status");
            AccountStatus status = AccountStatus.valueOf(statusStr.toUpperCase());
            if (status == AccountStatus.BANNED) {
                throw new AuthException("Votre compte a été banni.");
            }

            User user = new User();
            // HEX() returns hex string without dashes; reformat as UUID
            String hexId = rs.getString("id");
            user.setId(hexToUUID(hexId));
            user.setEmail(rs.getString("email"));
            user.setUsername(rs.getString("username"));
            user.setNickname(rs.getString("nickname"));
            user.setPassword(storedPassword);
            String rolesRaw = rs.getString("roles");
            if (rolesRaw != null && !rolesRaw.isBlank()) {
                user.setRoles(Arrays.asList(rolesRaw.split(",")));
            }
            user.setBalance(rs.getInt("balance"));
            user.setStatus(status);
            user.setIsVerified(rs.getBoolean("is_verified"));
            user.setDiscordId(rs.getString("discord_id"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

            return user;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new AuthException("Erreur de connexion à la base de données : " + e.getMessage());
        }
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────

    /**
     * Registers a new Joueur or Arbitre user.
     * For Arbitre, the licenseCode is validated against the license table.
     *
     * @param username    the pseudo
     * @param email       email
     * @param password    plain-text password
     * @param confirmPass confirmation must match password
     * @param isArbitre   true if account type is Arbitre
     * @param licenseCode only required when isArbitre == true
     * @throws AuthException on any validation or DB error
     */
    public User register(String username, String email,
                         String password, String confirmPass,
                         boolean isArbitre, String licenseCode) throws AuthException {

        // ── Validation ──
        if (username == null || username.isBlank())     throw new AuthException("Le pseudo ne peut pas être vide.");
        if (email == null || email.isBlank())           throw new AuthException("L'email ne peut pas être vide.");
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) throw new AuthException("Format d'email invalide.");
        if (password == null || password.length() < 6)  throw new AuthException("Le mot de passe doit contenir au moins 6 caractères.");
        if (!password.equals(confirmPass))               throw new AuthException("Les mots de passe ne correspondent pas.");
        if (isArbitre && (licenseCode == null || licenseCode.isBlank())) {
            throw new AuthException("Le numéro de licence est requis pour les arbitres.");
        }

        try {
            // ── Check email uniqueness ──
            String checkSql = "SELECT COUNT(*) FROM user WHERE email = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
                ps.setString(1, email.trim());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new AuthException("Cet email est déjà utilisé.");
                }
            }

            // ── Validate & claim license for Arbitre ──
            UUID licenseId = null;
            if (isArbitre) {
                String licSql = "SELECT HEX(id) as id, is_used FROM license WHERE license_code = ?";
                try (PreparedStatement ps = connection.prepareStatement(licSql)) {
                    ps.setString(1, licenseCode.trim());
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) throw new AuthException("Num\u00e9ro de licence introuvable.");
                    if (rs.getBoolean("is_used")) throw new AuthException("Cette licence est d\u00e9j\u00e0 utilis\u00e9e.");
                    licenseId = hexToUUID(rs.getString("id"));
                }
            }

            // ── Insert user ──
            UUID newId = UUID.randomUUID();
            // Symfony stores roles as JSON array e.g. ["ROLE_USER"]
            String role = isArbitre ? "ROLE_ARBITRE" : "ROLE_USER";
            String rolesJson = "[\"" + role + "\"]";
            // Hash the password with bcrypt before storing
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            // Use UNHEX to store UUID as binary(16) — Symfony/Doctrine format
            String insertSql = "INSERT INTO user (id, email, username, nickname, password, roles, balance, status, is_verified, created_at) " +
                               "VALUES (UNHEX(REPLACE(?, '-', '')), ?, ?, ?, ?, ?, 0, 'ACTIVE', 0, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, newId.toString());
                ps.setString(2, email.trim());
                ps.setString(3, username.trim());
                ps.setString(4, username.trim());  // nickname defaults to username
                ps.setString(5, hashedPassword);
                ps.setString(6, rolesJson);
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }

            // ── Mark license as used (Arbitre only) ──
            if (isArbitre && licenseId != null) {
                String updateLic = "UPDATE license SET is_used = 1, used_at = ?, assigned_to_id = UNHEX(REPLACE(?, '-', '')) WHERE id = UNHEX(REPLACE(?, '-', ''))";
                try (PreparedStatement ps = connection.prepareStatement(updateLic)) {
                    ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(2, newId.toString());
                    ps.setString(3, licenseId.toString());
                    ps.executeUpdate();
                }
            }

            // ── Return the freshly created user ──
            User user = new User();
            user.setId(newId);
            user.setEmail(email.trim());
            user.setUsername(username.trim());
            user.setNickname(username.trim());
            user.setPassword(hashedPassword);
            user.setRoles(Arrays.asList(role));
            user.setBalance(0);
            user.setStatus(AccountStatus.ACTIVE);
            user.setIsVerified(false);
            user.setCreatedAt(LocalDateTime.now());
            return user;

        } catch (SQLException e) {
            e.printStackTrace(); // print full stack trace so we can see the real DB error
            throw new AuthException("Erreur base de données : " + e.getMessage());
        }
    }

    // ─── Inner exception ─────────────────────────────────────────────────────

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Convert a 32-char hex string (from HEX() SQL function) to a proper UUID.
     * MySQL HEX() returns uppercase hex without dashes, e.g. "550E8400E29B41D4A716446655440000"
     */
    private static UUID hexToUUID(String hex) {
        if (hex == null || hex.length() != 32) {
            throw new IllegalArgumentException("Invalid hex UUID: " + hex);
        }
        String withDashes = hex.substring(0, 8) + "-" +
                            hex.substring(8, 12) + "-" +
                            hex.substring(12, 16) + "-" +
                            hex.substring(16, 20) + "-" +
                            hex.substring(20);
        return UUID.fromString(withDashes);
    }
}
