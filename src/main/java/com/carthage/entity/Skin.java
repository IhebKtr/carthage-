package com.carthage.entity;

import com.carthage.entity.enums.SkinRarity;
import com.carthage.entity.enums.SkinType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Skin {

    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private int price;
    private SkinRarity rarity;
    private SkinType skinType;
    private int stock;
    private String apiProvider;
    private String deliveryMethod;
    private LocalDateTime createdAt;
    private Game game;

    public Skin() {}

    public Skin(UUID id, String name, String description, String imageUrl, int price, SkinRarity rarity, SkinType skinType, int stock, String apiProvider, String deliveryMethod, LocalDateTime createdAt, Game game) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.rarity = rarity;
        this.skinType = skinType;
        this.stock = stock;
        this.apiProvider = apiProvider;
        this.deliveryMethod = deliveryMethod;
        this.createdAt = createdAt;
        this.game = game;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public SkinRarity getRarity() {
        return rarity;
    }

    public void setRarity(SkinRarity rarity) {
        this.rarity = rarity;
    }

    public SkinType getSkinType() {
        return skinType;
    }

    public void setSkinType(SkinType skinType) {
        this.skinType = skinType;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skin that = (Skin) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(imageUrl, that.imageUrl) &&
                price == that.price &&
                Objects.equals(rarity, that.rarity) &&
                Objects.equals(skinType, that.skinType) &&
                stock == that.stock &&
                Objects.equals(apiProvider, that.apiProvider) &&
                Objects.equals(deliveryMethod, that.deliveryMethod) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(game, that.game);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, imageUrl, price, rarity, skinType, stock, apiProvider, deliveryMethod, createdAt, game);
    }
}
