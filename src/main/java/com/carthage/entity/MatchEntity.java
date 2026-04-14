package com.carthage.entity;

import com.carthage.entity.enums.MatchStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class MatchEntity {

    private UUID id;
    private int round;
    private MatchStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Tournoi tournoi;
    private Team team1;
    private Team team2;
    private Team winner;

    public MatchEntity() {}

    public MatchEntity(UUID id, int round, MatchStatus status, LocalDateTime scheduledAt, LocalDateTime startedAt, LocalDateTime completedAt, String score, LocalDateTime createdAt, LocalDateTime updatedAt, Tournoi tournoi, Team team1, Team team2, Team winner) {
        this.id = id;
        this.round = round;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.score = score;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tournoi = tournoi;
        this.team1 = team1;
        this.team2 = team2;
        this.winner = winner;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
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

    public Tournoi getTournoi() {
        return tournoi;
    }

    public void setTournoi(Tournoi tournoi) {
        this.tournoi = tournoi;
    }

    public Team getTeam1() {
        return team1;
    }

    public void setTeam1(Team team1) {
        this.team1 = team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public void setTeam2(Team team2) {
        this.team2 = team2;
    }

    public Team getWinner() {
        return winner;
    }

    public void setWinner(Team winner) {
        this.winner = winner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchEntity that = (MatchEntity) o;
        return Objects.equals(id, that.id) &&
                round == that.round &&
                Objects.equals(status, that.status) &&
                Objects.equals(scheduledAt, that.scheduledAt) &&
                Objects.equals(startedAt, that.startedAt) &&
                Objects.equals(completedAt, that.completedAt) &&
                Objects.equals(score, that.score) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(tournoi, that.tournoi) &&
                Objects.equals(team1, that.team1) &&
                Objects.equals(team2, that.team2) &&
                Objects.equals(winner, that.winner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, round, status, scheduledAt, startedAt, completedAt, score, createdAt, updatedAt, tournoi, team1, team2, winner);
    }
}
