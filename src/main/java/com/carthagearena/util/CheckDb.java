package com.carthagearena.util;

import java.sql.*;

public class CheckDb {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main",
            "carthage-arena", "Carthage1122");

        System.out.println("=== TABLES ===");
        ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
        while (rs.next()) System.out.println("  " + rs.getString("TABLE_NAME"));
        rs.close();

        // Check if 'user' table exists and show its structure
        try {
            System.out.println("\n=== DESCRIBE user ===");
            Statement st = conn.createStatement();
            ResultSet r2 = st.executeQuery("DESCRIBE user");
            while (r2.next()) {
                System.out.println("  " + r2.getString("Field") + " | " + r2.getString("Type") + " | " + r2.getString("Null") + " | " + r2.getString("Key"));
            }
            r2.close(); st.close();
        } catch (Exception e) { System.out.println("  user table not found: " + e.getMessage()); }

        // Check if 'order' table exists
        try {
            System.out.println("\n=== DESCRIBE `order` ===");
            Statement st = conn.createStatement();
            ResultSet r2 = st.executeQuery("DESCRIBE `order`");
            while (r2.next()) {
                System.out.println("  " + r2.getString("Field") + " | " + r2.getString("Type") + " | " + r2.getString("Null") + " | " + r2.getString("Key"));
            }
            r2.close(); st.close();
        } catch (Exception e) { System.out.println("  order table not found: " + e.getMessage()); }

        conn.close();
    }
}
