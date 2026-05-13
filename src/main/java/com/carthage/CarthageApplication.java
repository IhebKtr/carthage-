package com.carthage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CarthageApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        com.carthage.services.EmailService.startScheduler(); // Démarrage du scheduler d'emails

        FXMLLoader fxmlLoader = new FXMLLoader(CarthageApplication.class.getResource("/com/carthage/view/user/login-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 900, 600);
        
        stage.setTitle("Carthage - Esports Management");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        com.carthage.services.EmailService.stopScheduler(); // Arrêt propre du scheduler
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
