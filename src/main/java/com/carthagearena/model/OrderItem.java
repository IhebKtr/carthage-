package com.carthagearena.model;

/**
 * Ligne de commande - associe un Merch à une quantité dans une Order
 */
public class OrderItem {

    private int id;
    private Merch merch;
    private int quantity;
    private int unitPrice;  // prix au moment de la commande (en centimes)

    public OrderItem() {}

    public OrderItem(Merch merch, int quantity) {
        this.merch = merch;
        this.quantity = quantity;
        this.unitPrice = merch.getPrice(); // snapshot du prix
    }

    public int getSubtotal() {
        return unitPrice * quantity;
    }

    public String getSubtotalFormatted() {
        return String.format("%.2f DT", getSubtotal() / 100.0);
    }

    public String getUnitPriceFormatted() {
        return String.format("%.2f DT", unitPrice / 100.0);
    }

    // ─── Getters & Setters ───────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Merch getMerch() { return merch; }
    public void setMerch(Merch merch) { this.merch = merch; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }
}
