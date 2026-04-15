package com.carthagearena.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestionnaire de connexion JDBC - Singleton pattern
 * Lit les credentials depuis le fichier .env
 */
public class DatabaseConnection {

    private static final String URL      = AppConfig.get("DB_URL",      "jdbc:mysql://localhost:3306/carthage_arena");
    private static final String USER     = AppConfig.get("DB_USER",     "root");
    private static final String PASSWORD = AppConfig.get("DB_PASSWORD", "");

    private static Connection instance = null;

    private DatabaseConnection() {}

    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à la base de données établie");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL introuvable : " + e.getMessage());
            }
        }
        return instance;
    }

    public static void closeConnection() {
        if (instance != null) {
            try {
                instance.close();
                instance = null;
                System.out.println("🔌 Connexion fermée");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
