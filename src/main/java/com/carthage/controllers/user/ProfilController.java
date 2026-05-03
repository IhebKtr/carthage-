package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class ProfilController {

    @FXML private Label usernameLabel, roleLabel, memberSinceLabel, bioLabel;
    @FXML private Label rankLabel, balanceLabel, emailLabel, avatarInitial;
    @FXML private Label matchesLabel, victoriesLabel, defaitesLabel;
    @FXML private Label winRateLabel, tournoiGagnesLabel, gainsLabel;
    @FXML private ProgressBar winRateProg;

    @FXML
    public void initialize() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) {
            usernameLabel.setText("Non connecté");
            return;
        }
        renderUser(user);
    }

    /**
     * Populate every header field from the given user. Extracted so it can be
     * reused after a successful profile edit without a full view reload.
     */
    private void renderUser(User user) {
        // ── Basic info ──
        String name = user.getUsername() != null ? user.getUsername() : "—";
        usernameLabel.setText(name);
        avatarInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
        emailLabel.setText(user.getEmail() != null ? user.getEmail() : "—");
        balanceLabel.setText(user.getBalance() + " CP");

        // ── Role badge ──
        boolean isAdmin   = hasRole(user, "ADMIN");
        boolean isArbitre = hasRole(user, "ARBITRE");
        if (isAdmin)        { roleLabel.setText("ADMINISTRATEUR"); }
        else if (isArbitre) { roleLabel.setText("ARBITRE"); }
        else                { roleLabel.setText("JOUEUR"); }

        // ── Member since ──
        if (user.getCreatedAt() != null) {
            memberSinceLabel.setText("📅 Membre depuis " +
                user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        }

        // ── Default stats (no match history table wired yet) ──
        matchesLabel.setText("0");
        victoriesLabel.setText("0");
        defaitesLabel.setText("0");
        winRateLabel.setText("0%");
        winRateProg.setProgress(0);
        tournoiGagnesLabel.setText("0");
        gainsLabel.setText("Gains totaux: 0 DT");
        rankLabel.setText("Unranked");
    }

    private boolean hasRole(User user, String keyword) {
        return user.getRoles() != null &&
            user.getRoles().stream().anyMatch(r -> r != null && r.toUpperCase().contains(keyword.toUpperCase()));
    }

    @FXML
    public void onEditProfil() {
        User user = SessionContext.getInstance().getCurrentUser();
        if (user == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/carthage/view/user/profil-edit-dialog.fxml"));
            Parent root = loader.load();

            ProfilEditController controller = loader.getController();
            controller.setUser(user);
            controller.setOnSaved(updated -> {
                renderUser(updated);
                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Profil mis à jour avec succès.");
                ok.setHeaderText(null);
                ok.showAndWait();
            });

            Stage dialog = new Stage();
            dialog.setTitle("Modifier le Profil");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(usernameLabel.getScene().getWindow());
            Scene scene = new Scene(root);
            // ESC closes the dialog (cancel without saving), with a discard-changes
            // confirmation if the form is dirty.
            scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    if (controller.confirmCloseIfDirty()) {
                        dialog.close();
                    }
                    ev.consume();
                }
            });
            // Same guard if the user clicks the OS window close button.
            dialog.setOnCloseRequest(ev -> {
                if (!controller.confirmCloseIfDirty()) {
                    ev.consume();
                }
            });
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR,
                "Impossible d'ouvrir l'éditeur de profil : " + e.getMessage());
            err.setHeaderText(null);
            err.showAndWait();
        }
    }

    @FXML public void onHistorique() {}
}
