package com.carthagearena.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplySchema {
    public static void main(String[] args) {
        String schemaPath = "src/main/resources/database/schema.sql";
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            if (conn == null) {
                System.err.println("❌ Impossible de se connecter à la base de données.");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                System.out.println("⏳ Désactivation des contraintes de clés étrangères...");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

                System.out.println("⏳ Récupération de la liste des tables...");
                List<String> tables = new ArrayList<>();
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }

                System.out.println("🗑️ Suppression de " + tables.size() + " tables existantes...");
                for (String table : tables) {
                    try {
                        stmt.execute("DROP TABLE IF EXISTS `" + table + "`");
                        System.out.println("  - Table `" + table + "` supprimée.");
                    } catch (Exception e) {
                        System.err.println("  ⚠️ Impossible de supprimer `" + table + "` : " + e.getMessage());
                    }
                }

                System.out.println("⏳ Lecture du fichier schema.sql...");
                String sql;
                try (BufferedReader reader = new BufferedReader(new FileReader(schemaPath))) {
                    sql = reader.lines()
                            .filter(line -> !line.trim().startsWith("--"))
                            .collect(Collectors.joining("\n"));
                }

                String[] statements = sql.split(";");
                for (String s : statements) {
                    String trimmed = s.trim();
                    if (trimmed.isEmpty()) continue;
                    
                    if (trimmed.toUpperCase().startsWith("CREATE DATABASE") || trimmed.toUpperCase().startsWith("USE ")) {
                        continue;
                    }

                    try {
                        // Supprimer les sauts de ligne pour un log plus propre
                        String logCmd = trimmed.substring(0, Math.min(trimmed.length(), 60)).replace("\n", " ");
                        System.out.println("🚀 Exécution : " + logCmd + "...");
                        stmt.execute(trimmed);
                    } catch (Exception e) {
                        System.err.println("❌ Erreur : " + e.getMessage());
                    }
                }

                System.out.println("⏳ Réactivation des contraintes...");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            System.out.println("✅ Initialisation complète réussie !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
