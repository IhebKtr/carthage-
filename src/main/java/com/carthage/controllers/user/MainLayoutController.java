package com.carthage.controllers.user;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard, btnTournois, btnBoutique, btnEquipe, btnProfil, btnReclamations, btnParametres;
    @FXML private VBox chatOverlay;

    private Button activeButton;
    private boolean chatVisible = false;

    @FXML
    public void onChatToggle() {
        chatVisible = !chatVisible;
        if (chatOverlay != null) {
            chatOverlay.setVisible(chatVisible);
            chatOverlay.setManaged(chatVisible);
        }
    }

    @FXML
    public void initialize() {
        setActive(btnDashboard);
        loadView("dashboard-view.fxml");
        // We set this on the StackPane because it's always there
        contentArea.setUserData(this); 
    }

    @FXML public void onDashboardClicked()    { setActive(btnDashboard);    loadView("dashboard-view.fxml"); }
    @FXML public void onTournoisClicked()     { setActive(btnTournois);     loadView("tournois-view.fxml"); }
    @FXML public void onBoutiqueClicked()     { setActive(btnBoutique);     loadView("boutique-view.fxml"); }
    @FXML public void onEquipeClicked()       { setActive(btnEquipe);       loadView("equipe-view.fxml"); }
    @FXML public void onProfilClicked()       { setActive(btnProfil);       loadView("profil-view.fxml"); }
    @FXML public void onReclamationsClicked() { setActive(btnReclamations); loadView("reclamations-view.fxml"); }
    @FXML public void onParametresClicked()   { setActive(btnParametres);   loadView("dashboard-view.fxml"); }

    @FXML
    public void onLogoutClicked() {
        com.carthage.utils.SessionContext.getInstance().cleanSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/login-view.fxml"));
            Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) contentArea.getScene().getWindow();
            stage.setTitle("Carthage Arena – Connexion");
            stage.setScene(new javafx.scene.Scene(root, 1100, 700));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActive(Button btn) {
        List<Button> all = List.of(btnDashboard, btnTournois, btnBoutique, btnEquipe, btnProfil, btnReclamations, btnParametres);
        for (Button b : all) {
            b.getStyleClass().removeAll("active");
        }
        if (btn != null) btn.getStyleClass().add("active");
        activeButton = btn;
    }

    public void loadView(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/" + viewName));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadTournoiDetail(java.util.UUID tournoiId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/tournoi-detail-view.fxml"));
            Parent view = loader.load();
            
            TournoiDetailController controller = loader.getController();
            controller.setTournoi(tournoiId);
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSkinDetail(java.util.UUID skinId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/carthage/view/user/skin-detail-view.fxml"));
            Parent view = loader.load();
            
            SkinDetailController controller = loader.getController();
            controller.setSkin(skinId);
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
