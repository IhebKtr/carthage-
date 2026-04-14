import java.sql.*;

public class DebugReclamations {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
        String user = "carthage-arena";
        String pass = "Carthage1122";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("--- All Reclamations ---");
            String sql = "SELECT HEX(id) as id, subject, message, HEX(author_id) as author_id FROM reclamation";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.println("Recl ID: " + rs.getString("id"));
                    System.out.println("Subject: " + rs.getString("subject"));
                    System.out.println("Message: " + rs.getString("message"));
                    System.out.println("Author ID: " + rs.getString("author_id"));
                    System.out.println("---");
                }
            }
            
            System.out.println("\n--- Simulating Controller Query (User: admin) ---");
            String adminUuid = "019c5095-7e24-7ddc-8f45-b7faa0c1a7ea";
            String sqlSim = "SELECT subject FROM reclamation WHERE author_id = UNHEX(REPLACE(?, '-', ''))";
            try (PreparedStatement ps = conn.prepareStatement(sqlSim)) {
                ps.setString(1, adminUuid);
                ResultSet rs = ps.executeQuery();
                int count = 0;
                while (rs.next()) {
                    System.out.println("Found: " + rs.getString("subject"));
                    count++;
                }
                System.out.println("Total found: " + count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
