import java.sql.*;

public class CheckOtherColumns {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
        String user = "carthage-arena";
        String password = "Carthage1122";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData md = conn.getMetaData();
            
            System.out.println("--- Table: reclamation ---");
            ResultSet rs1 = md.getColumns(null, null, "reclamation", "%");
            while (rs1.next()) {
                System.out.println(rs1.getString("COLUMN_NAME") + " (" + rs1.getString("TYPE_NAME") + ")");
            }

            System.out.println("\n--- Table: reclamation_response ---");
            ResultSet rs2 = md.getColumns(null, null, "reclamation_response", "%");
            while (rs2.next()) {
                System.out.println(rs2.getString("COLUMN_NAME") + " (" + rs2.getString("TYPE_NAME") + ")");
            }

            System.out.println("\n--- Table: skin ---");
            ResultSet rs3 = md.getColumns(null, null, "skin", "%");
            while (rs3.next()) {
                System.out.println(rs3.getString("COLUMN_NAME") + " (" + rs3.getString("TYPE_NAME") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
