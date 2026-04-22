package com.carthage.services;

import com.carthage.utils.DatabaseConnection;
import com.carthage.utils.PasswordResetCodeUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PasswordResetService {

    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final int BCRYPT_COST = 12;
    private static final String EMAIL_REGEX = "^[\\w.+-]+@[\\w-]+\\.[\\w.]+$";

    private final Connection connection;
    private final EmailService emailService;

    public PasswordResetService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.emailService = new EmailService();
    }

    // ─── PUBLIC API ─────────────────────────────────────────────────────────

    public void requestReset(String email) throws ResetException {
        if (email == null || email.isBlank()) {
            throw new ResetException("Veuillez saisir votre email.");
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw new ResetException("Format d'email invalide.");
        }

        String trimmedEmail = email.trim();

        try {
            UUID userId = findUserIdByEmail(trimmedEmail);
            if (userId == null) {
                // Anti-enumeration : silence si l'email n'existe pas
                return;
            }

            String code = PasswordResetCodeUtil.generateCode();
            String hash = PasswordResetCodeUtil.hash(code);

            deleteExistingTokens(userId);
            insertNewToken(userId, hash);

            sendResetEmail(trimmedEmail, code);

        } catch (SQLException e) {
            throw new ResetException("Erreur base de données : " + e.getMessage(), e);
        } catch (EmailService.EmailException e) {
            throw new ResetException("Impossible d'envoyer l'email : " + e.getMessage(), e);
        }
    }

    public void confirmReset(String email, String code, String newPassword) throws ResetException {
        if (email == null || email.isBlank()) {
            throw new ResetException("Veuillez saisir votre email.");
        }
        if (code == null || code.isBlank()) {
            throw new ResetException("Veuillez saisir le code reçu par email.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResetException("Le nouveau mot de passe doit contenir au moins 6 caractères.");
        }

        String trimmedEmail = email.trim();
        String hashOfSubmittedCode = PasswordResetCodeUtil.hash(code.trim());

        try {
            UUID userId = findUserIdByEmail(trimmedEmail);
            if (userId == null) {
                throw new ResetException("Code invalide ou expiré.");
            }

            UUID tokenId = findValidToken(userId, hashOfSubmittedCode);
            if (tokenId == null) {
                throw new ResetException("Code invalide ou expiré.");
            }

            String bcryptHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(BCRYPT_COST));
            updateUserPassword(userId, bcryptHash);
            markTokenAsUsed(tokenId);

        } catch (SQLException e) {
            throw new ResetException("Erreur base de données : " + e.getMessage(), e);
        }
    }

    // ─── USER LOOKUP ────────────────────────────────────────────────────────

    private UUID findUserIdByEmail(String email) throws SQLException {
        String sql = "SELECT HEX(id) AS id FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return hexToUUID(rs.getString("id"));
        }
    }

    // ─── TOKEN OPERATIONS ───────────────────────────────────────────────────

    private UUID findValidToken(UUID userId, String hash) throws SQLException {
        String sql = "SELECT HEX(id) AS id FROM password_reset_token " +
                "WHERE user_id = UNHEX(REPLACE(?, '-', '')) " +
                "  AND token = ? " +
                "  AND expires_at > NOW() " +
                "  AND used_at IS NULL " +
                "ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            return hexToUUID(rs.getString("id"));
        }
    }

    private void deleteExistingTokens(UUID userId) throws SQLException {
        String sql = "DELETE FROM password_reset_token WHERE user_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.executeUpdate();
        }
    }

    private void insertNewToken(UUID userId, String hash) throws SQLException {
        String sql = "INSERT INTO password_reset_token (id, user_id, token, expires_at, created_at) " +
                "VALUES (UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), ?, " +
                "        NOW() + INTERVAL ? MINUTE, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, userId.toString());
            ps.setString(3, hash);
            ps.setInt(4, CODE_EXPIRY_MINUTES);
            ps.executeUpdate();
        }
    }

    private void markTokenAsUsed(UUID tokenId) throws SQLException {
        String sql = "UPDATE password_reset_token SET used_at = NOW() " +
                "WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tokenId.toString());
            ps.executeUpdate();
        }
    }

    // ─── USER UPDATE ────────────────────────────────────────────────────────

    private void updateUserPassword(UUID userId, String bcryptHash) throws SQLException {
        String sql = "UPDATE user SET password = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bcryptHash);
            ps.setString(2, userId.toString());
            ps.executeUpdate();
        }
    }

    // ─── EMAIL ──────────────────────────────────────────────────────────────

    private void sendResetEmail(String email, String code) throws EmailService.EmailException {
        String subject = "Carthage Arena – Code de réinitialisation";
        String body = "Bonjour,\n\n" +
                "Voici votre code de réinitialisation :\n\n" +
                "    " + code + "\n\n" +
                "Ce code expire dans " + CODE_EXPIRY_MINUTES + " minutes.\n" +
                "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
                "— L'équipe Carthage Arena";
        emailService.send(email, subject, body);
    }

    // ─── UTILITY ────────────────────────────────────────────────────────────

    private static UUID hexToUUID(String hex) {
        String withDashes = hex.substring(0, 8) + "-" +
                hex.substring(8, 12) + "-" +
                hex.substring(12, 16) + "-" +
                hex.substring(16, 20) + "-" +
                hex.substring(20);
        return UUID.fromString(withDashes);
    }

    // ─── EXCEPTION ──────────────────────────────────────────────────────────

    public static class ResetException extends Exception {
        public ResetException(String message) {
            super(message);
        }

        public ResetException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}