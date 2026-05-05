package com.carthagearena.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Représente un article dans le panier
 */
public class CartItem {
    private final Merch product;
    private final IntegerProperty quantity;

    public CartItem(Merch product, int quantity) {
        this.product = product;
        this.quantity = new SimpleIntegerProperty(quantity);
    }

    public Merch getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    public int getTotalPrice() {
        return product.getPrice() * getQuantity();
    }

    public String getTotalPriceFormatted() {
        return String.format("%.2f DT", getTotalPrice() / 100.0);
    }
}
