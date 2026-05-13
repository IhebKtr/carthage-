package com.carthage.entity;

import com.carthage.entity.enums.TournamentStatus;
import com.carthage.entity.enums.TournamentType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Tournoi {

    private UUID id;
    private String nom;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private int nbEquipesMax;
    private int prizePool;
    private TournamentStatus status;
    private TournamentType type;
    private String place;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Game game;
    private List<Team> teams;
    private List<MatchEntity> matches;
    private Team winner;
    private User referee;

    public Tournoi() {}

    public Tournoi(UUID id, String nom, LocalDateTime dateDebut, LocalDateTime dateFin, int nbEquipesMax, int prizePool, TournamentStatus status, TournamentType type, String place, LocalDateTime createdAt, LocalDateTime updatedAt, Game game, List<Team> teams, List<MatchEntity> matches, Team winner, User referee) {
        this.id = id;
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbEquipesMax = nbEquipesMax;
        this.prizePool = prizePool;
        this.status = status;
        this.type = type;
        this.place = place;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.game = game;
        this.teams = teams;
        this.matches = matches;
        this.winner = winner;
        this.referee = referee;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public int getNbEquipesMax() {
        return nbEquipesMax;
    }

    public void setNbEquipesMax(int nbEquipesMax) {
        this.nbEquipesMax = nbEquipesMax;
    }

    public int getPrizePool() {
        return prizePool;
    }

    public void setPrizePool(int prizePool) {
        this.prizePool = prizePool;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public TournamentType getType() {
        return type;
    }

    public void setType(TournamentType type) {
        this.type = type;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<MatchEntity> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchEntity> matches) {
        this.matches = matches;
    }

    public Team getWinner() {
        return winner;
    }

    public void setWinner(Team winner) {
        this.winner = winner;
    }

    public User getReferee() {
        return referee;
    }

    public void setReferee(User referee) {
        this.referee = referee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tournoi that = (Tournoi) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(nom, that.nom) &&
                Objects.equals(dateDebut, that.dateDebut) &&
                Objects.equals(dateFin, that.dateFin) &&
                nbEquipesMax == that.nbEquipesMax &&
                prizePool == that.prizePool &&
                Objects.equals(status, that.status) &&
                Objects.equals(type, that.type) &&
                Objects.equals(place, that.place) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(game, that.game) &&
                Objects.equals(teams, that.teams) &&
                Objects.equals(matches, that.matches) &&
                Objects.equals(winner, that.winner) &&
                Objects.equals(referee, that.referee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, dateDebut, dateFin, nbEquipesMax, prizePool, status, type, place, createdAt, updatedAt, game, teams, matches, winner, referee);
    }
}
