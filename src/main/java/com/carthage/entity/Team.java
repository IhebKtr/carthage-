package com.carthage.entity;

import com.carthage.entity.enums.TeamStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Team {

    private UUID id;
    private String name;
    private String tag;
    private String description;
    private TeamStatus status;
    private String inviteCode;
    private LocalDateTime createdAt;
    private User captain;
    private List<TeamMembership> members;

    public Team() {}

    public Team(UUID id, String name, String tag, String description, TeamStatus status, String inviteCode, LocalDateTime createdAt, User captain, List<TeamMembership> members) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.description = description;
        this.status = status;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
        this.captain = captain;
        this.members = members;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCaptain() {
        return captain;
    }

    public void setCaptain(User captain) {
        this.captain = captain;
    }

    public List<TeamMembership> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMembership> members) {
        this.members = members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team that = (Team) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(tag, that.tag) &&
                Objects.equals(description, that.description) &&
                Objects.equals(status, that.status) &&
                Objects.equals(inviteCode, that.inviteCode) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(captain, that.captain) &&
                Objects.equals(members, that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, tag, description, status, inviteCode, createdAt, captain, members);
    }
}
