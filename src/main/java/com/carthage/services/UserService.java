package com.carthage.services;

import com.carthage.entity.User;
import com.carthage.entity.enums.AccountStatus;
import com.carthage.services.DiscordOAuthService.DiscordIdentity;
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

        String sql = "SELECT HEX(id) as id, email, username, nickname, password, roles, balance, status, is_verified, discord_id, two_factor_secret, is_two_factor_enabled, created_at " +
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
            return mapUser(rs);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new AuthException("Erreur de connexion à la base de données : " + e.getMessage());
        }
    }

    public User loginOrRegisterWithDiscord(DiscordIdentity identity) throws AuthException {
        if (identity == null) throw new AuthException("Profil Discord invalide.");

        String discordId = identity.discordId().trim();
        String email = identity.email().trim();
        String username = identity.username().trim();
        boolean verifiedEmail = identity.emailVerified();

        if (discordId.isBlank()) throw new AuthException("Identifiant Discord manquant.");
        if (email.isBlank()) throw new AuthException("Email Discord manquant.");
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) throw new AuthException("Email Discord invalide.");
        if (!verifiedEmail) throw new AuthException("Votre email Discord doit être vérifié.");
        if (username.isBlank()) username = "discord_user";

        try {
            User byDiscord = findUserByDiscordId(discordId);
            if (byDiscord != null) {
                if (byDiscord.getStatus() == AccountStatus.BANNED) {
                    throw new AuthException("Votre compte a été banni.");
                }
                return byDiscord;
            }

            User byEmail = findUserByEmail(email);
            if (byEmail != null) {
                if (byEmail.getStatus() == AccountStatus.BANNED) {
                    throw new AuthException("Votre compte a été banni.");
                }
                if (byEmail.getDiscordId() != null && !byEmail.getDiscordId().isBlank() &&
                        !byEmail.getDiscordId().equals(discordId)) {
                    throw new AuthException("Cet email est déjà lié à un autre compte Discord.");
                }
                linkDiscordToUser(byEmail.getId(), discordId, verifiedEmail);
                return loadUserById(byEmail.getId());
            }

            User created = createUserFromDiscord(email, username, discordId, verifiedEmail);
            return loadUserById(created.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new AuthException("Erreur base de données : " + e.getMessage());
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

    // ─── UPDATE PROFILE ──────────────────────────────────────────────────────

    /**
     * Updates the username, nickname, email, and (optionally) the password of
     * an existing user. All password-related fields must be provided together
     * if a password change is requested.
     *
     * Roles, balance, status, is_verified, discord_id, created_at and id are
     * intentionally NOT writable here.
     *
     * @param userId           id of the user to update (required)
     * @param username         new username (required, non-blank, 3..32 chars)
     * @param nickname         new nickname (optional, max 32 chars; falls back to username if blank)
     * @param email            new email (required, must match the same regex as register())
     * @param currentPassword  current plain-text password — required iff newPassword is provided
     * @param newPassword      new plain-text password (>= 6 chars) — optional
     * @return the updated User reflecting the new values
     * @throws AuthException on any validation, uniqueness, or DB error
     */
    public User updateProfile(UUID userId,
                              String username,
                              String nickname,
                              String email,
                              String currentPassword,
                              String newPassword) throws AuthException {

        // ── Basic argument validation ──
        if (userId == null) throw new AuthException("Utilisateur invalide.");

        String trimmedUsername = username == null ? "" : username.trim();
        String trimmedNickname = nickname == null ? "" : nickname.trim();
        String trimmedEmail    = email == null ? "" : email.trim();

        if (trimmedUsername.isBlank())                throw new AuthException("Le pseudo ne peut pas être vide.");
        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 32)
            throw new AuthException("Le pseudo doit contenir entre 3 et 32 caractères.");
        if (!trimmedNickname.isBlank() && trimmedNickname.length() > 32)
            throw new AuthException("Le nickname ne peut pas dépasser 32 caractères.");
        if (trimmedEmail.isBlank())                   throw new AuthException("L'email ne peut pas être vide.");
        if (!trimmedEmail.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"))
            throw new AuthException("Format d'email invalide.");

        // ── Password change is optional but, if requested, fully validated ──
        boolean wantsPasswordChange =
                (currentPassword != null && !currentPassword.isEmpty()) ||
                (newPassword != null && !newPassword.isEmpty());

        if (wantsPasswordChange) {
            if (currentPassword == null || currentPassword.isEmpty())
                throw new AuthException("Veuillez saisir votre mot de passe actuel.");
            if (newPassword == null || newPassword.length() < 6)
                throw new AuthException("Le nouveau mot de passe doit contenir au moins 6 caractères.");
            if (currentPassword.equals(newPassword))
                throw new AuthException("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        // Nickname is optional on UPDATE: a blank field means "no nickname" and is
        // persisted as SQL NULL. (Unlike register(), we do NOT silently fall back
        // to username — that would prevent the user from ever clearing it.)
        String finalNickname = trimmedNickname.isBlank() ? null : trimmedNickname;

        try {
            // ── Load the current row (need stored hash for password verification + current email) ──
            String storedHash;
            String currentEmail;
            String selectSql = "SELECT email, password FROM user WHERE id = UNHEX(REPLACE(?, '-', ''))";
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setString(1, userId.toString());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new AuthException("Compte introuvable.");
                currentEmail = rs.getString("email");
                storedHash   = rs.getString("password");
            }

            // ── Verify current password if a change was requested ──
            if (wantsPasswordChange) {
                String normalizedHash = storedHash != null && storedHash.startsWith("$2y$")
                        ? "$2a$" + storedHash.substring(4)
                        : storedHash;
                if (normalizedHash == null || !BCrypt.checkpw(currentPassword, normalizedHash)) {
                    throw new AuthException("Mot de passe actuel incorrect.");
                }
            }

            // ── If email changed, ensure it is not already used by another account ──
            if (!trimmedEmail.equalsIgnoreCase(currentEmail)) {
                String dupSql = "SELECT COUNT(*) FROM user WHERE email = ? AND id != UNHEX(REPLACE(?, '-', ''))";
                try (PreparedStatement ps = connection.prepareStatement(dupSql)) {
                    ps.setString(1, trimmedEmail);
                    ps.setString(2, userId.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new AuthException("Cet email est déjà utilisé.");
                    }
                }
            }

            // ── Persist ──
            String newHash = wantsPasswordChange
                    ? BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
                    : null;

            String updateSql = wantsPasswordChange
                    ? "UPDATE user SET username = ?, nickname = ?, email = ?, password = ? " +
                      "WHERE id = UNHEX(REPLACE(?, '-', ''))"
                    : "UPDATE user SET username = ?, nickname = ?, email = ? " +
                      "WHERE id = UNHEX(REPLACE(?, '-', ''))";

            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, trimmedUsername);
                if (finalNickname == null) {
                    ps.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(2, finalNickname);
                }
                ps.setString(3, trimmedEmail);
                if (wantsPasswordChange) {
                    ps.setString(4, newHash);
                    ps.setString(5, userId.toString());
                } else {
                    ps.setString(4, userId.toString());
                }
                int affected = ps.executeUpdate();
                if (affected == 0) throw new AuthException("Aucune mise à jour effectuée.");
            }

            // ── Return a fresh in-memory snapshot for the caller (SessionContext) ──
            User updated = new User();
            updated.setId(userId);
            updated.setUsername(trimmedUsername);
            updated.setNickname(finalNickname);
            updated.setEmail(trimmedEmail);
            updated.setPassword(wantsPasswordChange ? newHash : storedHash);
            return updated;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new AuthException("Erreur de connexion à la base de données : " + e.getMessage());
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

    private User loadUserById(UUID userId) throws SQLException, AuthException {
        String sql = "SELECT HEX(id) as id, email, username, nickname, password, roles, balance, status, is_verified, discord_id, two_factor_secret, is_two_factor_enabled, created_at " +
                "FROM user WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new AuthException("Compte introuvable.");
            }
            return mapUser(rs);
        }
    }

    private User findUserByDiscordId(String discordId) throws SQLException {
        String sql = "SELECT HEX(id) as id, email, username, nickname, password, roles, balance, status, is_verified, discord_id, two_factor_secret, is_two_factor_enabled, created_at " +
                "FROM user WHERE discord_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapUser(rs);
        }
    }

    private User findUserByEmail(String email) throws SQLException {
        String sql = "SELECT HEX(id) as id, email, username, nickname, password, roles, balance, status, is_verified, discord_id, two_factor_secret, is_two_factor_enabled, created_at " +
                "FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapUser(rs);
        }
    }

    private void linkDiscordToUser(UUID userId, String discordId, boolean verifiedEmail) throws SQLException {
        String sql = "UPDATE user SET discord_id = ?, is_verified = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ps.setBoolean(2, verifiedEmail);
            ps.setString(3, userId.toString());
            ps.executeUpdate();
        }
    }

    public void update2FASettings(UUID userId, String secret, boolean isEnabled) throws AuthException {
        String sql = "UPDATE user SET two_factor_secret = ?, is_two_factor_enabled = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, secret);
            ps.setBoolean(2, isEnabled);
            ps.setString(3, userId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AuthException("Erreur lors de la mise à jour des paramètres 2FA.");
        }
    }

    private User createUserFromDiscord(String email, String username, String discordId, boolean verifiedEmail) throws SQLException {
        UUID newId = UUID.randomUUID();
        String role = "ROLE_USER";
        String rolesJson = "[\"" + role + "\"]";
        String safeUsername = username.length() > 32 ? username.substring(0, 32) : username;
        String randomPasswordHash = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(12));

        String insertSql = "INSERT INTO user (id, email, username, nickname, password, roles, balance, status, is_verified, discord_id, created_at) " +
                "VALUES (UNHEX(REPLACE(?, '-', '')), ?, ?, ?, ?, ?, 0, 'ACTIVE', ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, newId.toString());
            ps.setString(2, email);
            ps.setString(3, safeUsername);
            ps.setString(4, safeUsername);
            ps.setString(5, randomPasswordHash);
            ps.setString(6, rolesJson);
            ps.setBoolean(7, verifiedEmail);
            ps.setString(8, discordId);
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }

        User created = new User();
        created.setId(newId);
        return created;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        String hexId = rs.getString("id");
        user.setId(hexToUUID(hexId));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setNickname(rs.getString("nickname"));
        user.setPassword(rs.getString("password"));
        String rolesRaw = rs.getString("roles");
        if (rolesRaw != null && !rolesRaw.isBlank()) {
            String stripped = rolesRaw.trim()
                    .replaceAll("^\\[|\\]$", "")
                    .replaceAll("\"", "");
            if (!stripped.isBlank()) {
                user.setRoles(Arrays.asList(stripped.split(",")));
            }
        }
        user.setBalance(rs.getInt("balance"));
        String statusStr = rs.getString("status");
        user.setStatus(statusStr == null ? AccountStatus.ACTIVE : AccountStatus.valueOf(statusStr.toUpperCase()));
        user.setIsVerified(rs.getBoolean("is_verified"));
        user.setDiscordId(rs.getString("discord_id"));
        user.setTwoFactorSecret(rs.getString("two_factor_secret"));
        user.setIsTwoFactorEnabled(rs.getBoolean("is_two_factor_enabled"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        return user;
    }
}
