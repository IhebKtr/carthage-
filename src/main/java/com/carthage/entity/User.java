package com.carthage.entity;

import com.carthage.entity.enums.AccountStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {

    private UUID id;
    private String email;
    private String username;
    private String nickname;
    private String password;
    private List<String> roles;
    private int balance;
    private AccountStatus status;
    private boolean isVerified;
    private String discordId;
    private LocalDateTime createdAt;
    private License license;
    private Profile profile;
    private AuthToken authToken;
    private List<TeamMembership> teamMemberships;
    private List<Purchase> purchases;

    public User() {}

    public User(UUID id, String email, String username, String nickname, String password, List<String> roles, int balance, AccountStatus status, boolean isVerified, String discordId, LocalDateTime createdAt, License license, Profile profile, AuthToken authToken, List<TeamMembership> teamMemberships, List<Purchase> purchases) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.roles = roles;
        this.balance = balance;
        this.status = status;
        this.isVerified = isVerified;
        this.discordId = discordId;
        this.createdAt = createdAt;
        this.license = license;
        this.profile = profile;
        this.authToken = authToken;
        this.teamMemberships = teamMemberships;
        this.purchases = purchases;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public boolean isIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public AuthToken getAuthToken() {
        return authToken;
    }

    public void setAuthToken(AuthToken authToken) {
        this.authToken = authToken;
    }

    public List<TeamMembership> getTeamMemberships() {
        return teamMemberships;
    }

    public void setTeamMemberships(List<TeamMembership> teamMemberships) {
        this.teamMemberships = teamMemberships;
    }

    public List<Purchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<Purchase> purchases) {
        this.purchases = purchases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(email, that.email) &&
                Objects.equals(username, that.username) &&
                Objects.equals(nickname, that.nickname) &&
                Objects.equals(password, that.password) &&
                Objects.equals(roles, that.roles) &&
                balance == that.balance &&
                Objects.equals(status, that.status) &&
                isVerified == that.isVerified &&
                Objects.equals(discordId, that.discordId) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(license, that.license) &&
                Objects.equals(profile, that.profile) &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(teamMemberships, that.teamMemberships) &&
                Objects.equals(purchases, that.purchases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, username, nickname, password, roles, balance, status, isVerified, discordId, createdAt, license, profile, authToken, teamMemberships, purchases);
    }
}
