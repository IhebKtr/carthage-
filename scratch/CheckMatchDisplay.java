import java.sql.*;
import java.time.format.DateTimeFormatter;

/**
 * Simulate exactly what TournoiDetailController.loadMatches() does.
 * Checks that matches load correctly per tournament.
 */
public class CheckMatchDisplay {
    static final String URL  = "jdbc:mysql://mysql-carthage-arena.alwaysdata.net:3306/carthage-arena_main";
    static final String USER = "carthage-arena";
    static final String PASS = "Carthage1122";
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

            // 1. List all tournaments that HAVE matches
            System.out.println("=== TOURNAMENTS WITH MATCHES ===");
            String tourSql = "SELECT HEX(t.id) as tid, t.nom, COUNT(m.id) as match_count " +
                             "FROM tournoi t " +
                             "JOIN match_game m ON m.tournoi_id = t.id " +
                             "GROUP BY t.id, t.nom";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(tourSql)) {
                while (rs.next()) {
                    System.out.printf("  [%s]  %s  —  %d match(s)%n",
                        rs.getString("tid").substring(0, 8),
                        rs.getString("nom"),
                        rs.getInt("match_count"));
                }
            }

            // 2. For each tournament, show matches exactly as the UI would
            System.out.println("\n=== MATCH CARDS (as rendered in UI) ===");
            String matchSql =
                "SELECT HEX(m.id) as id, m.round, m.status, m.scheduled_at, m.score, " +
                "t1.name as team1_name, t2.name as team2_name, w.name as winner_name, " +
                "HEX(m.tournoi_id) as tid, tour.nom as tournoi_nom " +
                "FROM match_game m " +
                "LEFT JOIN team t1 ON m.team1_id = t1.id " +
                "LEFT JOIN team t2 ON m.team2_id = t2.id " +
                "LEFT JOIN team w  ON m.winner_id = w.id " +
                "JOIN tournoi tour ON m.tournoi_id = tour.id " +
                "ORDER BY m.tournoi_id, m.round ASC, m.scheduled_at ASC";

            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(matchSql)) {
                String lastTid = "";
                int lastRound = -1;
                while (rs.next()) {
                    String tid = rs.getString("tid");
                    int round  = rs.getInt("round");

                    if (!tid.equals(lastTid)) {
                        System.out.println("\n  TOURNOI: " + rs.getString("tournoi_nom"));
                        lastTid   = tid;
                        lastRound = -1;
                    }
                    if (round != lastRound) {
                        System.out.println("    ── Round " + round + " ──");
                        lastRound = round;
                    }

                    String t1     = rs.getString("team1_name");
                    String t2     = rs.getString("team2_name");
                    String status = rs.getString("status");
                    Timestamp dt  = rs.getTimestamp("scheduled_at");
                    String score  = rs.getString("score");
                    String winner = rs.getString("winner_name");

                    System.out.printf("      [%s]  %-20s VS  %-20s  status=%-12s  date=%s  score=%s  winner=%s%n",
                        rs.getString("id").substring(0, 8),
                        t1 != null ? t1 : "TBD",
                        t2 != null ? t2 : "TBD",
                        status,
                        dt != null ? dt.toLocalDateTime().format(FMT) : "--",
                        score != null ? score : "--",
                        winner != null ? winner : "--");
                }
            }
        }
    }
}
