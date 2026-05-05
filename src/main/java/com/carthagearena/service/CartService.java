package com.carthagearena.service;

import com.carthagearena.model.CartItem;
import com.carthagearena.model.Merch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Service centralisant le panier d'achat (Singleton)
 */
public class CartService {
    private static CartService instance;
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();

    private CartService() {}

    public static synchronized CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public ObservableList<CartItem> getItems() {
        return items;
    }

    public void addProduct(Merch product) {
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }
        items.add(new CartItem(product, 1));
    }

    public void removeProduct(Merch product) {
        items.removeIf(item -> item.getProduct().getId().equals(product.getId()));
    }

    public void clear() {
        items.clear();
    }

    public int getTotalCents() {
        return items.stream()
                .mapToInt(CartItem::getTotalPrice)
                .sum();
    }

    public String getTotalFormatted() {
        return String.format("%.2f DT", getTotalCents() / 100.0);
    }

    public int getItemCount() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}
