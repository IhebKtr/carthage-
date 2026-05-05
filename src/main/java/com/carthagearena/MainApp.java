package com.carthagearena;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("🏟️ Carthage Arena");
        primaryStage.setScene(scene);
        
        // Ajustement des dimensions pour éviter le "zoom" excessif sur petits écrans
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        
        // Optionnel : Démarrer en plein écran pour tout voir
        primaryStage.setMaximized(true);
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
