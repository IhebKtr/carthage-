package com.carthage.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Purchase {

    private UUID id;
    private int quantity;
    private int totalPrice;
    private LocalDateTime purchaseDate;
    private Merch merch;
    private User user;

    public Purchase() {}

    public Purchase(UUID id, int quantity, int totalPrice, LocalDateTime purchaseDate, Merch merch, User user) {
        this.id = id;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.purchaseDate = purchaseDate;
        this.merch = merch;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Merch getMerch() {
        return merch;
    }

    public void setMerch(Merch merch) {
        this.merch = merch;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Purchase that = (Purchase) o;
        return Objects.equals(id, that.id) &&
                quantity == that.quantity &&
                totalPrice == that.totalPrice &&
                Objects.equals(purchaseDate, that.purchaseDate) &&
                Objects.equals(merch, that.merch) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, quantity, totalPrice, purchaseDate, merch, user);
    }
}
