package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.services.UserService;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller for the "Modifier le Profil" modal dialog.
 *
 * Wiring contract (set by the opener before showAndWait):
 *   - call {@link #setUser(User)} with the currently authenticated user
 *   - optionally call {@link #setOnSaved(Consumer)} to receive the updated User
 *     after a successful save (used by ProfilController to refresh its labels).
 */
public class ProfilEditController {

    @FXML private TextField usernameField;
    @FXML private TextField nicknameField;
    @FXML private TextField emailField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final UserService userService = new UserService();
    private User user;
    private Consumer<User> onSaved;

    // Snapshot of the prefilled values so we can detect dirty state on close.
    // These are set by setUser() and never mutated afterwards.
    private String originalUsername = "";
    private String originalNickname = "";
    private String originalEmail = "";

    @FXML
    private void initialize() {
        // Enter-key navigation: walk forward, last field submits.
        usernameField.setOnAction(e -> nicknameField.requestFocus());
        nicknameField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> currentPasswordField.requestFocus());
        currentPasswordField.setOnAction(e -> newPasswordField.requestFocus());
        newPasswordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleSave());
    }

    /** Called by the opener to inject the user being edited and prefill fields. */
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            originalUsername = user.getUsername() != null ? user.getUsername() : "";
            originalNickname = user.getNickname() != null ? user.getNickname() : "";
            originalEmail    = user.getEmail()    != null ? user.getEmail()    : "";
            usernameField.setText(originalUsername);
            nicknameField.setText(originalNickname);
            emailField.setText(originalEmail);
        }
    }

    /**
     * Returns true if the user has typed anything that differs from the prefilled
     * values, OR has typed into any of the password fields. Used by the opener
     * (and the close handlers below) to decide whether to prompt before closing.
     */
    public boolean isDirty() {
        if (!safe(usernameField.getText()).equals(originalUsername)) return true;
        if (!safe(nicknameField.getText()).equals(originalNickname)) return true;
        if (!safe(emailField.getText()).equals(originalEmail))       return true;
        if (!safe(currentPasswordField.getText()).isEmpty())          return true;
        if (!safe(newPasswordField.getText()).isEmpty())              return true;
        if (!safe(confirmPasswordField.getText()).isEmpty())          return true;
        return false;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * If the form is dirty, ask the user to confirm discarding their changes.
     * Returns true if the dialog should proceed to close, false to stay open.
     * If the form is clean, returns true immediately (no prompt).
     */
    public boolean confirmCloseIfDirty() {
        if (!isDirty()) return true;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Modifications non enregistrées");
        confirm.setHeaderText(null);
        confirm.setContentText("Vous avez des modifications non enregistrées. Voulez-vous vraiment fermer sans enregistrer ?");
        ButtonType discard = new ButtonType("Abandonner les modifications");
        ButtonType keep    = new ButtonType("Continuer l'édition", ButtonType.CANCEL.getButtonData());
        confirm.getButtonTypes().setAll(discard, keep);

        // Make the modal a child of our dialog so it stays on top.
        Stage owner = (Stage) usernameField.getScene().getWindow();
        if (owner != null) confirm.initOwner(owner);

        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == discard;
    }

    /** Optional callback fired with the updated User on a successful save. */
    public void setOnSaved(Consumer<User> onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void handleSave() {
        clearMessage();

        if (user == null || user.getId() == null) {
            showError("Session invalide. Veuillez vous reconnecter.");
            return;
        }

        String currentPwd = currentPasswordField.getText();
        String newPwd     = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        // Local pre-check: if the user typed a new password, the confirm must match.
        // (UserService also validates everything else server-side.)
        boolean wantsPasswordChange =
                (currentPwd != null && !currentPwd.isEmpty()) ||
                (newPwd != null && !newPwd.isEmpty()) ||
                (confirmPwd != null && !confirmPwd.isEmpty());

        if (wantsPasswordChange) {
            if (newPwd == null || !newPwd.equals(confirmPwd)) {
                showError("Les nouveaux mots de passe ne correspondent pas.");
                return;
            }
        }

        try {
            User updated = userService.updateProfile(
                    user.getId(),
                    usernameField.getText(),
                    nicknameField.getText(),
                    emailField.getText(),
                    wantsPasswordChange ? currentPwd : null,
                    wantsPasswordChange ? newPwd : null
            );

            // Carry over the read-only fields the service does not touch, so the
            // SessionContext snapshot stays complete (roles, balance, etc.).
            updated.setRoles(user.getRoles());
            updated.setBalance(user.getBalance());
            updated.setStatus(user.getStatus());
            updated.setIsVerified(user.isIsVerified());
            updated.setDiscordId(user.getDiscordId());
            updated.setCreatedAt(user.getCreatedAt());
            updated.setLicense(user.getLicense());
            updated.setProfile(user.getProfile());
            updated.setAuthToken(user.getAuthToken());
            updated.setTeamMemberships(user.getTeamMemberships());
            updated.setPurchases(user.getPurchases());

            SessionContext.getInstance().setCurrentUser(updated);

            if (onSaved != null) onSaved.accept(updated);
            closeDialog();

        } catch (UserService.AuthException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (confirmCloseIfDirty()) {
            closeDialog();
        }
    }

    /** Unconditional close — used after a successful save. */
    private void closeDialog() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #F87171; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void clearMessage() {
        messageLabel.setText("");
    }
}
