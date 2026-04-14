import java.sql.*;

public class CheckTeamMembership {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
        String user = "carthage-arena";
        String pass = "Carthage1122";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            DatabaseMetaData metaData = conn.getMetaData();

            String[] tables = { "team_membership", "team" };

            for (String tableName : tables) {
                System.out.println("--- Table: " + tableName + " ---");
                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String typeName = columns.getString("TYPE_NAME");
                    System.out.println(columnName + " (" + typeName + ")");
                }
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
