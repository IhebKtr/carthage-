package com.carthagearena.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité Merch - traduite depuis Symfony PHP vers Java
 * Représente un produit dérivé (merchandising) lié à Carthage Arena
 */
public class Merch {

    private UUID id;
    private String name;
    private String description;
    private int price;         // prix en centimes (ex : 1500 = 15.00 DT)
    private int stock;
    private String imageUrl;
    private String type;       // ex: "shirt", "cap", "jersey", "poster"
    private LocalDateTime createdAt;
    private Game game;         // relation ManyToOne

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public Merch() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }

    public Merch(String name, String description, int price, int stock,
                 String imageUrl, String type, Game game) {
        this();
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.type = type;
        this.game = game;
    }

    // Constructeur depuis base de données (avec id existant)
    public Merch(String id, String name, String description, int price, int stock,
                 String imageUrl, String type, LocalDateTime createdAt) {
        this.id = UUID.fromString(id);
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.type = type;
        this.createdAt = createdAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    /** Retourne le prix formaté en dinars (ex : 15.00 DT) */
    public String getPriceFormatted() {
        return String.format("%.2f DT", price / 100.0);
    }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public boolean isInStock() {
        return this.stock > 0;
    }

    @Override
    public String toString() {
        return name + " (" + getPriceFormatted() + ")";
    }
}
