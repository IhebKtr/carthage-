import java.sql.*;

public class CheckUserSkinSchema {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
        String user = "carthage-arena";
        String pass = "Carthage1122";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("--- Table: user_skin ---");
            ResultSet columns = metaData.getColumns(null, null, "user_skin", null);
            while (columns.next()) {
                System.out.println(columns.getString("COLUMN_NAME") + " (" + columns.getString("TYPE_NAME") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
