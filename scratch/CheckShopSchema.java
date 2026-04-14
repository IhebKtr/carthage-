import java.sql.*;

public class CheckShopSchema {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
        String user = "carthage-arena";
        String pass = "Carthage1122";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String[] tables = {"skin", "purchase", "article", "boutique"}; // Trying common names
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            System.out.println("--- All Tables ---");
            while (rs.next()) {
                System.out.println(rs.getString("TABLE_NAME"));
            }
            System.out.println("\n--- Investigating 'skin' ---");
            ResultSet columns = metaData.getColumns(null, null, "skin", null);
            while (columns.next()) {
                System.out.println(columns.getString("COLUMN_NAME") + " (" + columns.getString("TYPE_NAME") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
