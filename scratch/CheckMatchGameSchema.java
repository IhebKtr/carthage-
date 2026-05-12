import java.sql.*;

/**
 * Scratch: inspect match_game table columns + dump a few rows.
 * Run as a plain main class (add JDBC jar to classpath).
 */
public class CheckMatchGameSchema {
    static final String URL  = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
    static final String USER = "carthage-arena";
    static final String PASS = "Carthage1122";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

            // ── 1. Column names ──────────────────────────────────────
            System.out.println("=== match_game COLUMNS ===");
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet cols = meta.getColumns(null, null, "match_game", "%")) {
                while (cols.next()) {
                    System.out.printf("  %-20s  %s  nullable=%s%n",
                        cols.getString("COLUMN_NAME"),
                        cols.getString("TYPE_NAME"),
                        cols.getString("IS_NULLABLE"));
                }
            }

            // ── 2. Row count ─────────────────────────────────────────
            System.out.println("\n=== ROW COUNT ===");
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM match_game")) {
                if (rs.next()) System.out.println("  Total rows: " + rs.getInt(1));
            }

            // ── 3. Sample rows ───────────────────────────────────────
            System.out.println("\n=== SAMPLE ROWS (up to 5) ===");
            String sql = "SELECT HEX(id) as id, round, status, scheduled_at, score, " +
                         "HEX(tournoi_id) as tid, HEX(team1_id) as t1, HEX(team2_id) as t2, HEX(winner_id) as w " +
                         "FROM match_game LIMIT 5";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData rmd = rs.getMetaData();
                int cols2 = rmd.getColumnCount();
                while (rs.next()) {
                    System.out.print("  ");
                    for (int i = 1; i <= cols2; i++) {
                        System.out.print(rmd.getColumnLabel(i) + "=" + rs.getString(i) + " | ");
                    }
                    System.out.println();
                }
            }
        }
    }
}
