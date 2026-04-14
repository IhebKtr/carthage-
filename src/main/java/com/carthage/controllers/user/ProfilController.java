package com.carthage.controllers.user;

import com.carthage.entity.User;
import com.carthage.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    @FXML public void onEditProfil() {}
    @FXML public void onHistorique() {}
}
