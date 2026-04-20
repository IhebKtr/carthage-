package com.carthage.services;

import com.carthage.entity.MatchEntity;
import com.carthage.entity.Team;
import com.carthage.entity.Tournoi;
import com.carthage.entity.enums.MatchStatus;
import com.carthage.entity.enums.TournamentType;
import com.carthage.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MatchService {

    private final Connection connection;

    public MatchService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ─── Generate Bracket ────────────────────────────────────────────

    /**
     * Generates match records for a tournament based on its type and registered teams.
     * Round Robin: every team plays every other team once.
     * Single/Double Elimination: seeded bracket, round 1 pairs.
     */
    public List<MatchEntity> generateAndSaveMatches(Tournoi tournoi, List<Team> teams,
                                                     LocalDateTime firstMatchDate,
                                                     int minutesBetweenMatches) throws SQLException {
        List<MatchEntity> matches = new ArrayList<>();
        TournamentType type = tournoi.getType() != null ? tournoi.getType() : TournamentType.SINGLE_ELIMINATION;

        if (type == TournamentType.ROUND_ROBIN) {
            matches = buildRoundRobin(teams, firstMatchDate, minutesBetweenMatches);
        } else {
            // Single or Double Elimination — generate Round 1 bracket
            matches = buildEliminationRound1(teams, firstMatchDate, minutesBetweenMatches);
        }

        // Persist all generated matches
        for (MatchEntity m : matches) {
            m.setTournoi(tournoi);
            insertMatch(m);
        }
        return matches;
    }

    private List<MatchEntity> buildRoundRobin(List<Team> teams, LocalDateTime firstMatchDate,
                                               int minutesBetweenMatches) {
        List<MatchEntity> matches = new ArrayList<>();
        LocalDateTime scheduled = firstMatchDate;
        int round = 1;
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                MatchEntity m = new MatchEntity();
                m.setTeam1(teams.get(i));
                m.setTeam2(teams.get(j));
                m.setRound(round++);
                m.setStatus(MatchStatus.SCHEDULED);
                m.setScheduledAt(scheduled);
                matches.add(m);
                scheduled = scheduled.plusMinutes(minutesBetweenMatches);
            }
        }
        return matches;
    }

    private List<MatchEntity> buildEliminationRound1(List<Team> teams, LocalDateTime firstMatchDate,
                                                      int minutesBetweenMatches) {
        List<MatchEntity> matches = new ArrayList<>();
        List<Team> seeded = new ArrayList<>(teams);
        // Shuffle for random seeding
        Collections.shuffle(seeded);

        LocalDateTime scheduled = firstMatchDate;
        int matchNum = 1;
        for (int i = 0; i + 1 < seeded.size(); i += 2) {
            MatchEntity m = new MatchEntity();
            m.setTeam1(seeded.get(i));
            m.setTeam2(seeded.get(i + 1));
            m.setRound(1);
            m.setStatus(MatchStatus.SCHEDULED);
            m.setScheduledAt(scheduled);
            matches.add(m);
            scheduled = scheduled.plusMinutes(minutesBetweenMatches);
            matchNum++;
        }
        // If odd team count, last team gets a bye (no match generated for them until round 2)
        return matches;
    }

    // ─── DB Operations ───────────────────────────────────────────────

    private void insertMatch(MatchEntity m) throws SQLException {
        String sql = "INSERT INTO match_game (id, round, status, scheduled_at, tournoi_id, team1_id, team2_id, created_at, updated_at) " +
                     "VALUES (UNHEX(REPLACE(UUID(),'-','')), ?, ?, ?, UNHEX(REPLACE(?,'-','')), UNHEX(REPLACE(?,'-','')), UNHEX(REPLACE(?,'-','')), NOW(), NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, m.getRound());
            ps.setString(2, m.getStatus().name());
            ps.setTimestamp(3, m.getScheduledAt() != null ? Timestamp.valueOf(m.getScheduledAt()) : null);
            ps.setString(4, m.getTournoi().getId().toString());
            ps.setString(5, m.getTeam1().getId().toString());
            ps.setString(6, m.getTeam2().getId().toString());
            ps.executeUpdate();
        }
    }

    /** Return all matches for a given tournament. */
    public List<MatchEntity> getMatchesByTournament(UUID tournoiId) {
        List<MatchEntity> list = new ArrayList<>();
        String sql = "SELECT HEX(m.id) as id, m.round, m.status, m.scheduled_at, m.score, " +
                     "HEX(m.team1_id) as t1id, HEX(m.team2_id) as t2id, HEX(m.winner_id) as wid, " +
                     "t1.name as t1name, t2.name as t2name, w.name as wname " +
                     "FROM match_game m " +
                     "LEFT JOIN team t1 ON m.team1_id = t1.id " +
                     "LEFT JOIN team t2 ON m.team2_id = t2.id " +
                     "LEFT JOIN team w ON m.winner_id = w.id " +
                     "WHERE m.tournoi_id = UNHEX(REPLACE(?, '-', '')) " +
                     "ORDER BY m.round ASC, m.scheduled_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tournoiId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MatchEntity m = new MatchEntity();
                m.setId(hexToUUID(rs.getString("id")));
                m.setRound(rs.getInt("round"));
                try { m.setStatus(MatchStatus.valueOf(rs.getString("status").toUpperCase())); }
                catch (Exception e) { m.setStatus(MatchStatus.SCHEDULED); }
                if (rs.getTimestamp("scheduled_at") != null)
                    m.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                m.setScore(rs.getString("score"));

                Team t1 = new Team();
                t1.setId(hexToUUID(rs.getString("t1id")));
                t1.setName(rs.getString("t1name"));
                m.setTeam1(t1);

                Team t2 = new Team();
                t2.setId(hexToUUID(rs.getString("t2id")));
                t2.setName(rs.getString("t2name"));
                m.setTeam2(t2);

                String wid = rs.getString("wid");
                if (wid != null) {
                    Team w = new Team();
                    w.setId(hexToUUID(wid));
                    w.setName(rs.getString("wname"));
                    m.setWinner(w);
                }
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Update match result (score + winner). */
    public void updateMatchResult(UUID matchId, String score, Team winner) throws SQLException {
        String sql = "UPDATE match_game SET score=?, winner_id=UNHEX(REPLACE(?,'-','')), status=?, updated_at=NOW() " +
                     "WHERE id=UNHEX(REPLACE(?,'-',''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, score);
            ps.setString(2, winner != null && winner.getId() != null ? winner.getId().toString() : null);
            ps.setString(3, winner != null ? MatchStatus.COMPLETED.name() : MatchStatus.SCHEDULED.name());
            ps.setString(4, matchId.toString());
            ps.executeUpdate();
        }
    }

    /** Update match date (for drag and drop scheduling). */
    public void updateMatchDate(UUID matchId, LocalDateTime newDate) throws SQLException {
        String sql = "UPDATE match_game SET scheduled_at=?, updated_at=NOW() WHERE id=UNHEX(REPLACE(?,'-',''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, newDate != null ? Timestamp.valueOf(newDate) : null);
            ps.setString(2, matchId.toString());
            ps.executeUpdate();
        }
    }

    /** Delete all matches for a tournament (to regenerate). */
    public void deleteMatchesByTournament(UUID tournoiId) throws SQLException {
        String sql = "DELETE FROM match_game WHERE tournoi_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tournoiId.toString());
            ps.executeUpdate();
        }
    }

    /** Check if a tournament already has matches. */
    public boolean hasMatches(UUID tournoiId) {
        String sql = "SELECT COUNT(*) FROM match_game WHERE tournoi_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tournoiId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ─── Utility ─────────────────────────────────────────────────────

    private static UUID hexToUUID(String hex) {
        if (hex == null || hex.length() != 32) return null;
        String withDashes = hex.substring(0, 8) + "-" +
                            hex.substring(8, 12) + "-" +
                            hex.substring(12, 16) + "-" +
                            hex.substring(16, 20) + "-" +
                            hex.substring(20);
        return UUID.fromString(withDashes);
    }
}
