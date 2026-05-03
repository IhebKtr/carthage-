package com.carthage.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class PasswordResetToken {

    private UUID id;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private User user;
    private LocalDateTime usedAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(UUID id, String token, LocalDateTime expiresAt, LocalDateTime createdAt, User user) {
        this.id = id;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PasswordResetToken that = (PasswordResetToken) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(token, that.token) &&
                Objects.equals(expiresAt, that.expiresAt) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(user, that.user) &&
                Objects.equals(usedAt, that.usedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token, expiresAt, createdAt, user, usedAt);
    }
}
