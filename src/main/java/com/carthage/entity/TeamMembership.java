package com.carthage.entity;

import com.carthage.entity.enums.TeamRole;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class TeamMembership {

    private UUID id;
    private TeamRole role;
    private LocalDateTime joinedAt;
    private Team team;
    private User player;

    public TeamMembership() {}

    public TeamMembership(UUID id, TeamRole role, LocalDateTime joinedAt, Team team, User player) {
        this.id = id;
        this.role = role;
        this.joinedAt = joinedAt;
        this.team = team;
        this.player = player;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMembership that = (TeamMembership) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(role, that.role) &&
                Objects.equals(joinedAt, that.joinedAt) &&
                Objects.equals(team, that.team) &&
                Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role, joinedAt, team, player);
    }
}
