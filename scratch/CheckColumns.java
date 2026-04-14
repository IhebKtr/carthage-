import java.sql.*;

public class CheckColumns {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
        String user = "carthage-arena";
        String password = "Carthage1122";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData md = conn.getMetaData();
            
            System.out.println("--- Table: tournoi_team ---");
            ResultSet rs1 = md.getColumns(null, null, "tournoi_team", "%");
            while (rs1.next()) {
                System.out.println(rs1.getString("COLUMN_NAME") + " (" + rs1.getString("TYPE_NAME") + ")");
            }

            System.out.println("\n--- Table: match_game ---");
            ResultSet rs2 = md.getColumns(null, null, "match_game", "%");
            while (rs2.next()) {
                System.out.println(rs2.getString("COLUMN_NAME") + " (" + rs2.getString("TYPE_NAME") + ")");
            }
            
            System.out.println("\n--- Table: team ---");
            ResultSet rs3 = md.getColumns(null, null, "team", "%");
            while (rs3.next()) {
                System.out.println(rs3.getString("COLUMN_NAME") + " (" + rs3.getString("TYPE_NAME") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
