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
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private Button sendButton;

    private final PasswordResetService service = new PasswordResetService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    @FXML
    public void onSendCodeClicked() {
        clearFeedback();
        final String email = emailField.getText();
        if (email == null || email.isBlank()) {
            showError("Veuillez saisir votre email.");
            return;
        }

        sendButton.setDisable(true);
        sendButton.setText("Envoi en cours...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                service.requestReset(email);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            sendButton.setDisable(false);
            sendButton.setText("Envoyer le code");
            showSuccess("Si cet email existe, un code à 6 chiffres a été envoyé.");

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(ev -> openResetView(email));
            pause.play();
        });

        task.setOnFailed(e -> {
            sendButton.setDisable(false);
            sendButton.setText("Envoyer le code");
            Throwable ex = task.getException();
            if (ex instanceof PasswordResetService.ResetException) {
                showError(ex.getMessage());
            } else if (ex != null) {
                showError("Erreur inattendue : " + ex.getMessage());
            } else {
                showError("Erreur inattendue.");
            }
        });

        Thread t = new Thread(task, "forgot-password-request");
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

    private void openResetView(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/reset-password-view.fxml"));
            Parent root = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.setEmail(email);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle("Carthage Arena – Réinitialisation");
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