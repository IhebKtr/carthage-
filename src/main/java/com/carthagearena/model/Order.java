package com.carthagearena.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité Order (Commande) - Pourquoi utiliser Order ?
 *
 *  ✅ Un utilisateur peut acheter plusieurs tickets/produits en une seule fois
 *  ✅ Il faut regrouper ces achats
 *  ✅ Il faut garder une trace (historique des commandes)
 *
 *  📦 Structure :
 *   - id           : identifiant unique
 *   - date         : date de création de la commande
 *   - totalAmount  : montant total en centimes
 *   - status       : PENDING | PAID | CANCELLED
 *   - userId       : référence à l'utilisateur
 *   - items        : liste des lignes de commande (OrderItem)
 */
public class Order {

    public enum Status {
        PENDING("En attente"),
        PAID("Payée"),
        CANCELLED("Annulée");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    private UUID id;
    private LocalDateTime date;
    private int totalAmount;       // en centimes
    private Status status;
    private int userId;
    private String userFullName;   // pour affichage
    private List<OrderItem> items;

    // ─── Constructeurs ───────────────────────────────────────────────────────

    public Order() {
        this.id = UUID.randomUUID();
        this.date = LocalDateTime.now();
        this.status = Status.PENDING;
        this.items = new ArrayList<>();
        this.totalAmount = 0;
    }

    public Order(int userId, String userFullName) {
        this();
        this.userId = userId;
        this.userFullName = userFullName;
    }

    // Constructeur depuis base de données
    public Order(String id, LocalDateTime date, int totalAmount,
                 Status status, int userId, String userFullName) {
        this.id = UUID.fromString(id);
        this.date = date;
        this.totalAmount = totalAmount;
        this.status = status;
        this.userId = userId;
        this.userFullName = userFullName;
        this.items = new ArrayList<>();
    }

    // ─── Méthodes métier ─────────────────────────────────────────────────────

    /**
     * Ajoute un produit à la commande et recalcule le total
     */
    public void addItem(Merch merch, int quantity) {
        OrderItem existing = items.stream()
                .filter(i -> i.getMerch().getId().equals(merch.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.add(new OrderItem(merch, quantity));
        }
        recalculateTotal();
    }

    /**
     * Retire un produit de la commande
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        recalculateTotal();
    }

    /**
     * Recalcule le montant total
     */
    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .mapToInt(i -> i.getMerch().getPrice() * i.getQuantity())
                .sum();
    }

    public String getTotalFormatted() {
        return String.format("%.2f DT", totalAmount / 100.0);
    }

    public boolean canBePaid() {
        return status == Status.PENDING && !items.isEmpty();
    }

    public boolean canBeCancelled() {
        return status == Status.PENDING;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) {
        this.items = items;
        recalculateTotal();
    }

    @Override
    public String toString() {
        return "Commande #" + id.toString().substring(0, 8)
                + " | " + status.getLabel()
                + " | " + getTotalFormatted();
    }
}
