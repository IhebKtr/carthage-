package com.carthage.entity;

import com.carthage.entity.enums.GameStatus;
import com.carthage.entity.enums.GameType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Game {

    private UUID id;
    private String name;
    private String description;
    private GameType type;
    private GameStatus status;
    private String imageUrl;
    private LocalDateTime createdAt;
    private List<Tournoi> tournois;
    private List<Skin> skins;

    public Game() {}

    public Game(UUID id, String name, String description, GameType type, GameStatus status, String imageUrl, LocalDateTime createdAt, List<Tournoi> tournois, List<Skin> skins) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.tournois = tournois;
        this.skins = skins;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GameType getType() {
        return type;
    }

    public void setType(GameType type) {
        this.type = type;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Tournoi> getTournois() {
        return tournois;
    }

    public void setTournois(List<Tournoi> tournois) {
        this.tournois = tournois;
    }

    public List<Skin> getSkins() {
        return skins;
    }

    public void setSkins(List<Skin> skins) {
        this.skins = skins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game that = (Game) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(type, that.type) &&
                Objects.equals(status, that.status) &&
                Objects.equals(imageUrl, that.imageUrl) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(tournois, that.tournois) &&
                Objects.equals(skins, that.skins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, type, status, imageUrl, createdAt, tournois, skins);
    }
}
