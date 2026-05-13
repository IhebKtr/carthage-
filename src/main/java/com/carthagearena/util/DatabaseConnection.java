package com.carthagearena.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestionnaire de connexion JDBC - Singleton pattern
 * Connexion vers la base de données distante AlwaysData
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    private final String URL      = AppConfig.get("DB_URL", "jdbc:mysql://localhost:3306/carthage_arena");
    private final String USERNAME = AppConfig.get("DB_USER", "root");
    private final String PASSWORD = AppConfig.get("DB_PASSWORD", "");

    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Connexion à la base de données établie !");
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("❌ Échec de connexion à la base de données.");
            e.printStackTrace();
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        } else {
            try {
                if (instance.getConnection() == null || instance.getConnection().isClosed()) {
                    instance = new DatabaseConnection();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}

