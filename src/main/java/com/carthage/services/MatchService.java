package com.carthage.services;

import com.carthage.entity.MatchEntity;
import com.carthage.entity.Team;
import com.carthage.entity.Tournoi;
import com.carthage.entity.enums.MatchStatus;
import com.carthage.entity.enums.TournamentType;
import com.carthage.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MatchService {

    private final Connection connection;

    public MatchService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ─── 🧮 CONFLICT RESOLUTION ENGINE (Auto-Fix Scheduler) ────────────

    private static class TimeSlot {
        final LocalDateTime start;
        final LocalDateTime end;
        TimeSlot(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
        boolean overlaps(TimeSlot other) {
            // Strict overlap check
            return this.start.isBefore(other.end) && other.start.isBefore(this.end);
        }
    }

    /**
     * Generates match records, actively checking a busy_slots matrix to avoid:
     * - Team double-booking
     * - Field (Place) overbooking
     */
    public List<MatchEntity> generateAndSaveMatches(Tournoi tournoi, List<Team> teams,
                                                     LocalDateTime firstMatchDate,
                                                     int minutesBetweenMatches) throws SQLException {
        List<MatchEntity> matches;
        TournamentType type = tournoi.getType() != null ? tournoi.getType() : TournamentType.SINGLE_ELIMINATION;

        // 1. Generate base matchups (without times)
        if (type == TournamentType.ROUND_ROBIN) {
            matches = buildRoundRobinBase(teams);
        } else {
            matches = buildEliminationRound1Base(teams);
        }

        // 2. Load global schedules to build busy_slots matrix
        List<MatchEntity> allGlobalMatches = getUpcomingMatchesAcrossAllTournaments(firstMatchDate.minusDays(1));
        
        Map<UUID, List<TimeSlot>> teamSchedule = new HashMap<>();
        Map<String, List<TimeSlot>> fieldSchedule = new HashMap<>();
        
        for (MatchEntity existing : allGlobalMatches) {
            if (existing.getScheduledAt() == null) continue;
            // We assume an existing match occupies minutesBetweenMatches (default duration)
            TimeSlot slot = new TimeSlot(existing.getScheduledAt(), existing.getScheduledAt().plusMinutes(minutesBetweenMatches));
            
            if (existing.getTeam1() != null && existing.getTeam1().getId() != null) 
                addSlot(teamSchedule, existing.getTeam1().getId(), slot);
            if (existing.getTeam2() != null && existing.getTeam2().getId() != null) 
                addSlot(teamSchedule, existing.getTeam2().getId(), slot);
            if (existing.getTournoi() != null && existing.getTournoi().getPlace() != null) 
                addSlot(fieldSchedule, existing.getTournoi().getPlace(), slot);
        }

        // 3. Sliding Window Conflict Resolution
        LocalDateTime currentAttempt = firstMatchDate;
        String place = tournoi.getPlace() != null ? tournoi.getPlace() : "Unknown";

        for (MatchEntity m : matches) {
            boolean scheduled = false;
            while (!scheduled) {
                TimeSlot proposed = new TimeSlot(currentAttempt, currentAttempt.plusMinutes(minutesBetweenMatches));
                boolean conflict = false;
                
                // 3a. Check Field availability
                if (hasConflict(fieldSchedule.get(place), proposed)) {
                    conflict = true;
                }
                
                // 3b. Check Team 1 availability
                if (!conflict && m.getTeam1() != null && hasConflict(teamSchedule.get(m.getTeam1().getId()), proposed)) {
                    conflict = true;
                }
                
                // 3c. Check Team 2 availability
                if (!conflict && m.getTeam2() != null && hasConflict(teamSchedule.get(m.getTeam2().getId()), proposed)) {
                    conflict = true;
                }

                if (conflict) {
                    // Push forward by 30 mins to find next available clear slot
                    currentAttempt = currentAttempt.plusMinutes(30); 
                } else {
                    // Slot is clear! Reserve it.
                    m.setScheduledAt(currentAttempt);
                    
                    addSlot(fieldSchedule, place, proposed);
                    if (m.getTeam1() != null) addSlot(teamSchedule, m.getTeam1().getId(), proposed);
                    if (m.getTeam2() != null) addSlot(teamSchedule, m.getTeam2().getId(), proposed);
                    
                    scheduled = true;
                    // Move target forward for the next match
                    currentAttempt = currentAttempt.plusMinutes(minutesBetweenMatches);
                }
            }
            
            // Persist the smartly scheduled match
            m.setTournoi(tournoi);
            insertMatch(m);
        }
        
        return matches;
    }

    private <K> void addSlot(Map<K, List<TimeSlot>> map, K key, TimeSlot slot) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
    }

    private boolean hasConflict(List<TimeSlot> existingSlots, TimeSlot proposed) {
        if (existingSlots == null) return false;
        for (TimeSlot existing : existingSlots) {
            if (existing.overlaps(proposed)) return true;
        }
        return false;
    }

    // ─── Base Matchup Generation (No Times) ──────────────────────────

    private List<MatchEntity> buildRoundRobinBase(List<Team> teams) {
        List<MatchEntity> matches = new ArrayList<>();
        int round = 1;
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                MatchEntity m = new MatchEntity();
                m.setTeam1(teams.get(i));
                m.setTeam2(teams.get(j));
                m.setRound(round++);
                m.setStatus(MatchStatus.SCHEDULED);
                matches.add(m);
            }
        }
        return matches;
    }

    private List<MatchEntity> buildEliminationRound1Base(List<Team> teams) {
        List<MatchEntity> matches = new ArrayList<>();
        List<Team> seeded = new ArrayList<>(teams);
        Collections.shuffle(seeded);
        for (int i = 0; i + 1 < seeded.size(); i += 2) {
            MatchEntity m = new MatchEntity();
            m.setTeam1(seeded.get(i));
            m.setTeam2(seeded.get(i + 1));
            m.setRound(1);
            m.setStatus(MatchStatus.SCHEDULED);
            matches.add(m);
        }
        return matches;
    }

    // ─── Global DB Queries for Scheduler ─────────────────────────────

    private List<MatchEntity> getUpcomingMatchesAcrossAllTournaments(LocalDateTime fromDate) {
        List<MatchEntity> list = new ArrayList<>();
        String sql = "SELECT HEX(m.id) as id, m.scheduled_at, HEX(m.team1_id) as t1id, HEX(m.team2_id) as t2id, t.place " +
                     "FROM match_game m " +
                     "JOIN tournoi t ON m.tournoi_id = t.id " +
                     "WHERE m.scheduled_at >= ? AND m.status IN ('SCHEDULED', 'ONGOING')";
                     
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(fromDate));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MatchEntity m = new MatchEntity();
                m.setId(hexToUUID(rs.getString("id")));
                if (rs.getTimestamp("scheduled_at") != null)
                    m.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                
                String t1id = rs.getString("t1id");
                if (t1id != null) { Team t1 = new Team(); t1.setId(hexToUUID(t1id)); m.setTeam1(t1); }
                
                String t2id = rs.getString("t2id");
                if (t2id != null) { Team t2 = new Team(); t2.setId(hexToUUID(t2id)); m.setTeam2(t2); }
                
                Tournoi tournoi = new Tournoi();
                tournoi.setPlace(rs.getString("place"));
                m.setTournoi(tournoi);
                
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Existing DB Operations ──────────────────────────────────────

    private void insertMatch(MatchEntity m) throws SQLException {
        String sql = "INSERT INTO match_game (id, round, status, scheduled_at, tournoi_id, team1_id, team2_id, created_at, updated_at) " +
                     "VALUES (UNHEX(REPLACE(UUID(),'-','')), ?, ?, ?, UNHEX(REPLACE(?,'-','')), UNHEX(REPLACE(?,'-','')), UNHEX(REPLACE(?,'-','')), NOW(), NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, m.getRound());
            ps.setString(2, m.getStatus().name());
            ps.setTimestamp(3, m.getScheduledAt() != null ? Timestamp.valueOf(m.getScheduledAt()) : null);
            ps.setString(4, m.getTournoi().getId().toString());
            ps.setString(5, m.getTeam1() != null ? m.getTeam1().getId().toString() : null);
            ps.setString(6, m.getTeam2() != null ? m.getTeam2().getId().toString() : null);
            ps.executeUpdate();
        }
    }

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

                String t1id = rs.getString("t1id");
                if (t1id != null) {
                    Team t1 = new Team();
                    t1.setId(hexToUUID(t1id));
                    t1.setName(rs.getString("t1name"));
                    m.setTeam1(t1);
                }

                String t2id = rs.getString("t2id");
                if (t2id != null) {
                    Team t2 = new Team();
                    t2.setId(hexToUUID(t2id));
                    t2.setName(rs.getString("t2name"));
                    m.setTeam2(t2);
                }

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

    public void updateMatchDate(UUID matchId, LocalDateTime newDate) throws SQLException {
        String sql = "UPDATE match_game SET scheduled_at=?, updated_at=NOW() WHERE id=UNHEX(REPLACE(?,'-',''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, newDate != null ? Timestamp.valueOf(newDate) : null);
            ps.setString(2, matchId.toString());
            ps.executeUpdate();
        }
    }

    public void deleteMatchesByTournament(UUID tournoiId) throws SQLException {
        String sql = "DELETE FROM match_game WHERE tournoi_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tournoiId.toString());
            ps.executeUpdate();
        }
    }

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
