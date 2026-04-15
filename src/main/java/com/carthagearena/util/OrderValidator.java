package com.carthagearena.util;

import com.carthagearena.model.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Validateur de saisie pour l'entité Order
 */
public class OrderValidator {

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String field, String message) {
            errors.add("❌ " + field + " : " + message);
        }

        public boolean isValid() { return errors.isEmpty(); }
        public List<String> getErrors() { return errors; }

        public String getErrorsSummary() {
            return String.join("\n", errors);
        }
    }

    public static ValidationResult validate(Order order) {
        ValidationResult result = new ValidationResult();

        if (order.getUserId() <= 0) {
            result.addError("Utilisateur", "L'identifiant de l'utilisateur est invalide");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            result.addError("Panier", "La commande doit contenir au moins un produit");
        }

        if (order.getTotalAmount() < 0) {
            result.addError("Total", "Le montant total ne peut pas être négatif");
        }

        if (order.getStatus() == null) {
            result.addError("Statut", "Le statut de la commande est obligatoire");
        }

        return result;
    }
}
