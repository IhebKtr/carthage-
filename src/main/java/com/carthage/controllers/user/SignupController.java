package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.services.DiscordOAuthService;
import com.carthage.services.UserService;
import com.carthage.utils.SessionContext;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SignupController {

    @FXML private ToggleGroup accountTypeGroup;
    @FXML private ToggleButton joueurToggle;
    @FXML private ToggleButton arbitreToggle;

    @FXML private TextField pseudoField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private VBox licenceContainer;
    @FXML private TextField licenceField;

    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        joueurToggle.setSelected(true);
        licenceContainer.setVisible(false);
        licenceContainer.setManaged(false);
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        accountTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // Prevent fully deselecting – reselect previous
                oldToggle.setSelected(true);
                return;
            }
            boolean arbitre = (newToggle == arbitreToggle);
            licenceContainer.setVisible(arbitre);
            licenceContainer.setManaged(arbitre);
        });

        // ── Enter-key navigation ──
        // Each text field forwards focus; the last field on the form submits.
        pseudoField.setOnAction(e -> emailField.requestFocus());
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> {
            // If Arbitre is selected, Enter on confirm jumps to the licence field;
            // otherwise it submits.
            if (arbitreToggle.isSelected()) {
                licenceField.requestFocus();
            } else {
                onSignUpClicked();
            }
        });
        licenceField.setOnAction(e -> onSignUpClicked());
    }

    @FXML
    public void onSignUpClicked() {
        clearMessages();

        boolean isArbitre = arbitreToggle.isSelected();
        String pseudo    = pseudoField.getText();
        String email     = emailField.getText();
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();
        String licence   = isArbitre ? licenceField.getText() : null;

        try {
            User user = userService.register(pseudo, email, password, confirm, isArbitre, licence);
            SessionContext.getInstance().setCurrentUser(user);

            showSuccess("Compte créé avec succès ! Redirection...");

            // Short delay then navigate to dashboard
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.2));
            pause.setOnFinished(e -> loadScene("/com/carthage/view/user/main-layout-view.fxml", "Carthage Arena – Dashboard"));
            pause.play();

        } catch (UserService.AuthException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void onDiscordSignUpClicked() {
        clearMessages();
        showSuccess("Ouverture de Discord dans votre navigateur...");

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                DiscordOAuthService discordOAuthService = new DiscordOAuthService();
                DiscordOAuthService.DiscordIdentity identity = discordOAuthService.authenticate();
                return userService.loginOrRegisterWithDiscord(identity);
            }
        };

        task.setOnSucceeded(e -> {
            User user = task.getValue();
            SessionContext.getInstance().setCurrentUser(user);
            loadScene("/com/carthage/view/user/main-layout-view.fxml", "Carthage Arena – Dashboard");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof UserService.AuthException ||
                    ex instanceof DiscordOAuthService.OAuthException ||
                    ex instanceof IllegalStateException) {
                showError(ex.getMessage());
            } else if (ex != null) {
                showError("Erreur Discord OAuth : " + ex.getMessage());
            } else {
                showError("Erreur Discord OAuth inattendue.");
            }
        });

        Thread t = new Thread(task, "discord-oauth-signup");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void onNavigateToSignIn() {
        loadScene("/com/carthage/view/user/login-view.fxml", "Carthage Arena – Connexion");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void showError(String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText("✔  " + message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1100, 700));
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Impossible de charger la page : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
