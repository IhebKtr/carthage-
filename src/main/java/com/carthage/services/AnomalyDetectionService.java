package com.carthage.services;

import com.carthage.entity.MatchEntity;
import com.carthage.entity.Team;
import com.carthage.entity.Tournoi;
import com.carthage.entity.enums.MatchStatus;
import com.carthage.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyses tournament and match data to detect suspicious or abnormal patterns.
 * All methods are pure-compute (no UI dependencies) so they can be called from
 * any controller without coupling.
 */
public class AnomalyDetectionService {

    // ── Thresholds ──────────────────────────────────────────────────────────
    /** A team is "dominant" if its win-rate exceeds this value (e.g. 90 %). */
    private static final double DOMINANT_WIN_RATE_THRESHOLD = 0.85;

    /** A score is "extreme" when the margin between the two halves is ≥ this. */
    private static final int EXTREME_SCORE_MARGIN = 15;

    /** Flag a tournament as "stalled" if it has no completed matches after 7 days. */
    private static final int STALL_DAYS = 7;

    // ────────────────────────────────────────────────────────────────────────

    private final Connection connection;
    private final MatchService matchService;

    public AnomalyDetectionService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.matchService = new MatchService();
    }

    // ── Public data model ───────────────────────────────────────────────────

    public enum Severity { HIGH, MEDIUM, LOW }

    /** One detected anomaly entry. */
    public static class Anomaly {
        public final Severity severity;
        public final String category;    // e.g. "Dominant Team"
        public final String title;
        public final String description;
        public final String tournamentName;

        public Anomaly(Severity severity, String category,
                       String title, String description, String tournamentName) {
            this.severity      = severity;
            this.category      = category;
            this.title         = title;
            this.description   = description;
            this.tournamentName = tournamentName;
        }
    }

    // ── Main entry point ────────────────────────────────────────────────────

    /**
     * Run all detectors against every completed/ongoing tournament and
     * return a flat list of anomalies, sorted by severity (HIGH first).
     */
    public List<Anomaly> detectAll(List<Tournoi> tournois) {
        List<Anomaly> anomalies = new ArrayList<>();

        for (Tournoi t : tournois) {
            List<MatchEntity> matches = matchService.getMatchesByTournament(t.getId());

            anomalies.addAll(detectDominantTeam(t, matches));
            anomalies.addAll(detectSuspiciousScores(t, matches));
            anomalies.addAll(detectStalledTournament(t, matches));
            anomalies.addAll(detectUnbalancedBracket(t, matches));
            anomalies.addAll(detectOneTeamTournament(t));
        }

        // Sort: HIGH → MEDIUM → LOW
        anomalies.sort(Comparator.comparingInt(a -> a.severity.ordinal()));
        return anomalies;
    }

    // ── Detector: Dominant Team ─────────────────────────────────────────────

    /**
     * Flags a team that wins every single match (≥ threshold) within a tournament.
     * Indicates a possible skill-mismatch, or manipulation.
     */
    private List<Anomaly> detectDominantTeam(Tournoi t, List<MatchEntity> matches) {
        List<Anomaly> out = new ArrayList<>();
        List<MatchEntity> completed = matches.stream()
                .filter(m -> m.getWinner() != null)
                .collect(Collectors.toList());

        if (completed.size() < 2) return out;   // not enough data

        // Count wins per team id
        Map<UUID, Integer> wins  = new HashMap<>();
        Map<UUID, String>  names = new HashMap<>();
        Map<UUID, Integer> played = new HashMap<>();

        for (MatchEntity m : completed) {
            Team winner = m.getWinner();
            wins.merge(winner.getId(), 1, Integer::sum);
            names.put(winner.getId(), winner.getName());

            countPlay(played, m.getTeam1());
            countPlay(played, m.getTeam2());
        }

        for (Map.Entry<UUID, Integer> e : wins.entrySet()) {
            int totalPlayed = played.getOrDefault(e.getKey(), 1);
            double rate = (double) e.getValue() / totalPlayed;
            if (rate >= DOMINANT_WIN_RATE_THRESHOLD && e.getValue() >= 2) {
                out.add(new Anomaly(
                    Severity.HIGH,
                    "Dominant Team",
                    "🏆 " + names.get(e.getKey()) + " wins " + e.getValue() + "/" + totalPlayed + " matches",
                    "Team \"" + names.get(e.getKey()) + "\" has an abnormally high win-rate of "
                        + String.format("%.0f%%", rate * 100)
                        + " in tournament \"" + t.getNom() + "\". Possible skill-mismatch or bracket manipulation.",
                    t.getNom()
                ));
            }
        }
        return out;
    }

    private void countPlay(Map<UUID, Integer> map, Team team) {
        if (team != null && team.getId() != null) {
            map.merge(team.getId(), 1, Integer::sum);
        }
    }

    // ── Detector: Suspicious / Extreme Scores ───────────────────────────────

    /**
     * Looks for scores with an extreme margin (e.g. "30-0") which may indicate
     * match fixing, smurf accounts, or data-entry errors.
     */
    private List<Anomaly> detectSuspiciousScores(Tournoi t, List<MatchEntity> matches) {
        List<Anomaly> out = new ArrayList<>();
        for (MatchEntity m : matches) {
            String score = m.getScore();
            if (score == null || score.isBlank()) continue;

            // Expected format: "X-Y"
            String[] parts = score.split("[-–]");
            if (parts.length != 2) continue;
            try {
                int a = Integer.parseInt(parts[0].trim());
                int b = Integer.parseInt(parts[1].trim());
                int margin = Math.abs(a - b);

                if (a == 0 || b == 0) {
                    // Perfect shut-out
                    out.add(new Anomaly(
                        Severity.MEDIUM,
                        "Suspicious Score",
                        "🎯 Perfect shut-out: " + score,
                        "Match in round " + m.getRound() + " of \"" + t.getNom()
                            + "\" ended " + score + " — a complete shut-out may warrant review.",
                        t.getNom()
                    ));
                } else if (margin >= EXTREME_SCORE_MARGIN) {
                    out.add(new Anomaly(
                        Severity.MEDIUM,
                        "Extreme Score Margin",
                        "📊 Large margin: " + score + " (Δ " + margin + ")",
                        "Match round " + m.getRound() + " of \"" + t.getNom()
                            + "\" shows an unusual score margin of " + margin + " points (" + score + ").",
                        t.getNom()
                    ));
                }
            } catch (NumberFormatException ignored) {
                // score format not numeric — flag as data quality issue
                out.add(new Anomaly(
                    Severity.LOW,
                    "Invalid Score Format",
                    "⚠️ Unreadable score: \"" + score + "\"",
                    "Round " + m.getRound() + " of \"" + t.getNom()
                        + "\" has a score value that cannot be parsed: \"" + score + "\".",
                    t.getNom()
                ));
            }
        }
        return out;
    }

    // ── Detector: Stalled Tournament ────────────────────────────────────────

    /**
     * An ONGOING tournament with zero completed matches after {@code STALL_DAYS}
     * days from its start date is considered stalled.
     */
    private List<Anomaly> detectStalledTournament(Tournoi t, List<MatchEntity> matches) {
        List<Anomaly> out = new ArrayList<>();

        if (t.getStatus() == null) return out;
        String statusName = t.getStatus().name();
        if (!"ONGOING".equalsIgnoreCase(statusName)) return out;

        boolean hasCompleted = matches.stream().anyMatch(m -> m.getWinner() != null);
        if (hasCompleted) return out;

        if (t.getDateDebut() != null) {
            long daysSinceStart = java.time.temporal.ChronoUnit.DAYS
                    .between(t.getDateDebut(), LocalDateTime.now());
            if (daysSinceStart >= STALL_DAYS) {
                out.add(new Anomaly(
                    Severity.HIGH,
                    "Stalled Tournament",
                    "⏳ No results after " + daysSinceStart + " days",
                    "Tournament \"" + t.getNom() + "\" has been ONGOING for " + daysSinceStart
                        + " days with zero completed matches. Possible administrative inaction.",
                    t.getNom()
                ));
            }
        }
        return out;
    }

    // ── Detector: Unbalanced Bracket ────────────────────────────────────────

    /**
     * In an elimination tournament, if a single team appears in more matches
     * than any round-robin expectation (played > rounds), this could indicate
     * bracket tampering.
     */
    private List<Anomaly> detectUnbalancedBracket(Tournoi t, List<MatchEntity> matches) {
        List<Anomaly> out = new ArrayList<>();
        if (matches.size() < 4) return out;

        Map<UUID, Integer> played = new HashMap<>();
        Map<UUID, String>  names  = new HashMap<>();

        for (MatchEntity m : matches) {
            addTeamCount(played, names, m.getTeam1());
            addTeamCount(played, names, m.getTeam2());
        }

        // Average appearances across all teams
        if (played.isEmpty()) return out;
        double avg = played.values().stream().mapToInt(i -> i).average().orElse(0);
        double threshold = avg * 2.5; // flag anyone with > 2.5× average appearances

        for (Map.Entry<UUID, Integer> e : played.entrySet()) {
            if (e.getValue() > threshold && e.getValue() > 3) {
                out.add(new Anomaly(
                    Severity.MEDIUM,
                    "Bracket Imbalance",
                    "🔀 " + names.get(e.getKey()) + " played " + e.getValue() + " matches",
                    "Team \"" + names.get(e.getKey()) + "\" participated in " + e.getValue()
                        + " matches, which is " + String.format("%.1f×", (double) e.getValue() / avg)
                        + " the average. Bracket may be unbalanced.",
                    t.getNom()
                ));
            }
        }
        return out;
    }

    private void addTeamCount(Map<UUID, Integer> map, Map<UUID, String> names, Team team) {
        if (team != null && team.getId() != null) {
            map.merge(team.getId(), 1, Integer::sum);
            names.putIfAbsent(team.getId(), team.getName() != null ? team.getName() : "?");
        }
    }

    // ── Detector: Insufficient Participants ─────────────────────────────────

    /**
     * A tournament with only one registered team cannot produce fair competition.
     */
    private List<Anomaly> detectOneTeamTournament(Tournoi t) {
        List<Anomaly> out = new ArrayList<>();
        int teamCount = t.getTeams() != null ? t.getTeams().size() : 0;
        if (teamCount == 1) {
            out.add(new Anomaly(
                Severity.MEDIUM,
                "Insufficient Participants",
                "👥 Only 1 team registered",
                "Tournament \"" + t.getNom() + "\" has only 1 team enrolled. "
                    + "A minimum of 2 teams is needed to run a fair competition.",
                t.getNom()
            ));
        } else if (teamCount == 0 && t.getStatus() != null
                   && !"UPCOMING".equalsIgnoreCase(t.getStatus().name())) {
            out.add(new Anomaly(
                Severity.HIGH,
                "Empty Tournament",
                "🚫 No teams — status is " + t.getStatus().name(),
                "Tournament \"" + t.getNom() + "\" has no registered teams but its status is "
                    + t.getStatus().name() + ". This is logically inconsistent.",
                t.getNom()
            ));
        }
        return out;
    }

    // ── Statistics helper used by the controller ─────────────────────────────

    /**
     * Returns a quick-stats map for header cards in the UI:
     *   total_anomalies, high_count, medium_count, low_count,
     *   tournaments_analysed, matches_analysed
     */
    public Map<String, Integer> quickStats(List<Anomaly> anomalies, int tournamentsAnalysed, int matchesAnalysed) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("total_anomalies",     anomalies.size());
        stats.put("high_count",          (int) anomalies.stream().filter(a -> a.severity == Severity.HIGH).count());
        stats.put("medium_count",        (int) anomalies.stream().filter(a -> a.severity == Severity.MEDIUM).count());
        stats.put("low_count",           (int) anomalies.stream().filter(a -> a.severity == Severity.LOW).count());
        stats.put("tournaments_analysed", tournamentsAnalysed);
        stats.put("matches_analysed",     matchesAnalysed);
        return stats;
    }
}
