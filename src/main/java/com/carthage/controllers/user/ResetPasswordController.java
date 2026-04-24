package com.carthage.controllers.user;

import com.carthage.services.PasswordResetService;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class ResetPasswordController {

    @FXML
    private TextField emailField;
    @FXML
    private TextField codeField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private Button resetButton;

    private final PasswordResetService service = new PasswordResetService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Enter-key navigation: walk forward through the form, last field submits.
        emailField.setOnAction(e -> codeField.requestFocus());
        codeField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> onResetClicked());
    }

    public void setEmail(String email) {
        if (email != null) {
            emailField.setText(email);
        }
    }

    @FXML
    public void onResetClicked() {
        clearFeedback();

        final String email = emailField.getText();
        final String code = codeField.getText();
        final String newPassword = passwordField.getText();
        final String confirmPassword = confirmPasswordField.getText();

        if (email == null || email.isBlank()) {
            showError("Veuillez saisir votre email.");
            return;
        }
        if (code == null || code.isBlank()) {
            showError("Veuillez saisir le code reçu par email.");
            return;
        }
        if (newPassword == null || newPassword.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        resetButton.setDisable(true);
        resetButton.setText("Réinitialisation...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                service.confirmReset(email, code, newPassword);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            resetButton.setDisable(false);
            resetButton.setText("Réinitialiser");
            showSuccess("Mot de passe modifié avec succès ! Redirection vers la connexion...");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.8));
            pause.setOnFinished(ev -> loadScene(
                    "/com/carthage/view/user/login-view.fxml",
                    "Carthage Arena – Connexion"));
            pause.play();
        });

        task.setOnFailed(e -> {
            resetButton.setDisable(false);
            resetButton.setText("Réinitialiser");
            Throwable ex = task.getException();
            if (ex instanceof PasswordResetService.ResetException) {
                showError(ex.getMessage());
            } else if (ex != null) {
                showError("Erreur inattendue : " + ex.getMessage());
            } else {
                showError("Erreur inattendue.");
            }
        });

        Thread t = new Thread(task, "reset-password-confirm");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void onBackToLogin() {
        loadScene("/com/carthage/view/user/login-view.fxml", "Carthage Arena – Connexion");
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

    private void showError(String message) {
        errorLabel.setText("\u26A0  " + message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText("\u2714  " + message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }

    private void clearFeedback() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }
}
