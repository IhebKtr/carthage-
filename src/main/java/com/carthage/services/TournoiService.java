package com.carthage.services;

import com.carthage.entity.Game;
import com.carthage.entity.Team;
import com.carthage.entity.Tournoi;
import com.carthage.entity.User;
import com.carthage.entity.enums.TournamentStatus;
import com.carthage.entity.enums.TournamentType;
import com.carthage.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TournoiService {

    private final Connection connection;

    public TournoiService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public List<Tournoi> getAllTournois() {
        List<Tournoi> list = new ArrayList<>();
        String sql = "SELECT HEX(t.id) as id, t.nom, t.date_debut, t.nb_equipes_max, t.prize_pool, t.status, t.type, " +
                     "g.name as game_name, " +
                     "(SELECT COUNT(*) FROM tournoi_team tt WHERE tt.tournoi_id = t.id) as active_teams " +
                     "FROM tournoi t " +
                     "LEFT JOIN game g ON t.game_id = g.id";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Tournoi t = new Tournoi();
                t.setId(hexToUUID(rs.getString("id")));
                t.setNom(rs.getString("nom"));
                if (rs.getTimestamp("date_debut") != null) {
                    t.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                }
                t.setNbEquipesMax(rs.getInt("nb_equipes_max"));
                t.setPrizePool(rs.getInt("prize_pool"));
                
                try { t.setStatus(TournamentStatus.valueOf(rs.getString("status").toUpperCase())); }
                catch (Exception e) { t.setStatus(TournamentStatus.UPCOMING); }
                try { t.setType(TournamentType.valueOf(rs.getString("type").toUpperCase())); }
                catch (Exception e) { t.setType(TournamentType.SINGLE_ELIMINATION); }

                Game g = new Game();
                g.setName(rs.getString("game_name") != null ? rs.getString("game_name") : "Jeu inconnu");
                t.setGame(g);

                List<Team> dummyTeams = new ArrayList<>();
                int teamsCount = rs.getInt("active_teams");
                for(int i = 0; i < teamsCount; i++) dummyTeams.add(new Team());
                t.setTeams(dummyTeams);

                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void create(Tournoi t) throws SQLException {
        String sql = "INSERT INTO tournoi (id, nom, date_debut, date_fin, nb_equipes_max, prize_pool, status, type, place, game_id, referee_id, created_at, updated_at) " +
                     "VALUES (UNHEX(REPLACE(UUID(), '-', '')), ?, ?, ?, ?, ?, ?, ?, ?, UNHEX(?), UNHEX(?), NOW(), NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getNom());
            ps.setTimestamp(2, t.getDateDebut() != null ? Timestamp.valueOf(t.getDateDebut()) : null);
            ps.setTimestamp(3, t.getDateFin() != null ? Timestamp.valueOf(t.getDateFin()) : null);
            ps.setInt(4, t.getNbEquipesMax());
            ps.setInt(5, t.getPrizePool());
            ps.setString(6, t.getStatus() != null ? t.getStatus().name() : TournamentStatus.UPCOMING.name());
            ps.setString(7, t.getType() != null ? t.getType().name() : TournamentType.SINGLE_ELIMINATION.name());
            ps.setString(8, t.getPlace());
            
            ps.setString(9, t.getGame() != null && t.getGame().getId() != null ? t.getGame().getId().toString().replace("-", "") : null);
            ps.setString(10, t.getReferee() != null && t.getReferee().getId() != null ? t.getReferee().getId().toString().replace("-", "") : null);
            
            ps.executeUpdate();
        }
    }

    public void update(Tournoi t) throws SQLException {
        String sql = "UPDATE tournoi SET nom=?, date_debut=?, date_fin=?, nb_equipes_max=?, prize_pool=?, status=?, type=?, place=?, game_id=UNHEX(?), referee_id=UNHEX(?), winner_id=UNHEX(?), updated_at=NOW() " +
                     "WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getNom());
            ps.setTimestamp(2, t.getDateDebut() != null ? Timestamp.valueOf(t.getDateDebut()) : null);
            ps.setTimestamp(3, t.getDateFin() != null ? Timestamp.valueOf(t.getDateFin()) : null);
            ps.setInt(4, t.getNbEquipesMax());
            ps.setInt(5, t.getPrizePool());
            ps.setString(6, t.getStatus() != null ? t.getStatus().name() : TournamentStatus.UPCOMING.name());
            ps.setString(7, t.getType() != null ? t.getType().name() : TournamentType.SINGLE_ELIMINATION.name());
            ps.setString(8, t.getPlace());
            
            ps.setString(9, t.getGame() != null && t.getGame().getId() != null ? t.getGame().getId().toString().replace("-", "") : null);
            ps.setString(10, t.getReferee() != null && t.getReferee().getId() != null ? t.getReferee().getId().toString().replace("-", "") : null);
            ps.setString(11, t.getWinner() != null && t.getWinner().getId() != null ? t.getWinner().getId().toString().replace("-", "") : null);
            
            ps.setString(12, t.getId().toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID id) throws SQLException {
        String sql = "DELETE FROM tournoi WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        }
    }

    public Tournoi getById(UUID id) {
        String sql = "SELECT HEX(t.id) as id, t.nom, t.date_debut, t.date_fin, t.nb_equipes_max, t.prize_pool, t.status, t.type, t.place, " +
                     "HEX(t.game_id) as game_id, HEX(t.referee_id) as referee_id, HEX(t.winner_id) as winner_id " +
                     "FROM tournoi t WHERE t.id = UNHEX(REPLACE(?, '-', ''))";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Tournoi t = new Tournoi();
                t.setId(hexToUUID(rs.getString("id")));
                t.setNom(rs.getString("nom"));
                if (rs.getTimestamp("date_debut") != null) t.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                if (rs.getTimestamp("date_fin") != null) t.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                t.setNbEquipesMax(rs.getInt("nb_equipes_max"));
                t.setPrizePool(rs.getInt("prize_pool"));
                
                try { t.setStatus(TournamentStatus.valueOf(rs.getString("status").toUpperCase())); }
                catch (Exception e) { t.setStatus(TournamentStatus.UPCOMING); }
                try { t.setType(TournamentType.valueOf(rs.getString("type").toUpperCase())); }
                catch (Exception e) { t.setType(TournamentType.SINGLE_ELIMINATION); }
                t.setPlace(rs.getString("place"));

                String gid = rs.getString("game_id");
                if (gid != null) { Game g = new Game(); g.setId(hexToUUID(gid)); t.setGame(g); }
                
                String rid = rs.getString("referee_id");
                if (rid != null) { User u = new User(); u.setId(hexToUUID(rid)); t.setReferee(u); }
                
                String wid = rs.getString("winner_id");
                if (wid != null) { Team te = new Team(); te.setId(hexToUUID(wid)); t.setWinner(te); }

                return t;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Game> getAllGames() {
        List<Game> games = new ArrayList<>();
        String sql = "SELECT HEX(id) as id, name FROM game";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Game g = new Game();
                g.setId(hexToUUID(rs.getString("id")));
                g.setName(rs.getString("name"));
                games.add(g);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return games;
    }

    public List<User> getReferees() {
        List<User> refs = new ArrayList<>();
        String sql = "SELECT HEX(id) as id, username FROM user"; // Modify if we only want ROLE_REF
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(hexToUUID(rs.getString("id")));
                u.setUsername(rs.getString("username"));
                refs.add(u);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return refs;
    }

    public List<Team> getTeamsForTournament(UUID tournoiId) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT HEX(t.id) as id, t.name FROM team t " +
                     "JOIN tournoi_team tt ON t.id = tt.team_id " +
                     "WHERE tt.tournoi_id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tournoiId.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Team t = new Team();
                t.setId(hexToUUID(rs.getString("id")));
                t.setName(rs.getString("name"));
                teams.add(t);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return teams;
    }

    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT HEX(id) as id, name FROM team";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Team t = new Team();
                t.setId(hexToUUID(rs.getString("id")));
                t.setName(rs.getString("name"));
                teams.add(t);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return teams;
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
