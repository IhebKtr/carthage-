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
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        // Enter on the email field → jump to the password field.
        emailField.setOnAction(e -> passwordField.requestFocus());

        // Enter on the password field → submit (same as clicking "Sign In").
        passwordField.setOnAction(e -> onSignInClicked());
    }

    @FXML
    public void onSignInClicked() {
        clearError();
        String email = emailField.getText();
        String password = passwordField.getText();

        try {
            User user = userService.login(email, password);
            SessionContext.getInstance().setCurrentUser(user);
            routeByRole(user);

        } catch (UserService.AuthException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void onNavigateToSignUp() {
        loadScene("/com/carthage/view/user/signup-view.fxml", "Carthage Arena – Inscription");
    }

    @FXML
    public void onForgotPasswordClicked() {
        loadScene("/com/carthage/view/user/forgot-password-view.fxml", "Carthage Arena – Mot de passe oublié");
    }

    @FXML
    public void onDiscordSignInClicked() {
        clearError();

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
            routeByRole(user);
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

        Thread t = new Thread(task, "discord-oauth-signin");
        t.setDaemon(true);
        t.start();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void showError(String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
    }

    private void routeByRole(User user) {
        boolean isAdmin = user.getRoles() != null &&
                user.getRoles().stream().anyMatch(r -> r.toUpperCase().contains("ADMIN"));

        if (isAdmin) {
            loadScene("/com/carthage/view/admin/main-layout-view.fxml", "Carthage Arena – Admin");
        } else {
            loadScene("/com/carthage/view/user/main-layout-view.fxml", "Carthage Arena – Dashboard");
        }
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
