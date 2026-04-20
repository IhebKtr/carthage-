package com.carthage;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

public class TestFxmlLoad {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(TestFxmlLoad.class.getResource("/com/carthage/view/admin/reclamations-view.fxml"));
                loader.load();
                System.out.println("SUCCESS!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
            }
        });
    }
}
