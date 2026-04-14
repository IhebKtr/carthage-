package com.carthage.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class UserSkin {

    private UUID id;
    private LocalDateTime purchasedAt;
    private String status;
    private User user;
    private Skin skin;

    public UserSkin() {}

    public UserSkin(UUID id, LocalDateTime purchasedAt, String status, User user, Skin skin) {
        this.id = id;
        this.purchasedAt = purchasedAt;
        this.status = status;
        this.user = user;
        this.skin = skin;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(LocalDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Skin getSkin() {
        return skin;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSkin that = (UserSkin) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(purchasedAt, that.purchasedAt) &&
                Objects.equals(status, that.status) &&
                Objects.equals(user, that.user) &&
                Objects.equals(skin, that.skin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, purchasedAt, status, user, skin);
    }
}
