package com.carthagearena.util;

import com.carthagearena.model.Merch;

import java.util.ArrayList;
import java.util.List;

/**
 * Validateur de saisie pour l'entité Merch
 * Équivalent des Assert Symfony côté Java
 */
public class MerchValidator {

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String field, String message) {
            errors.add("❌ " + field + " : " + message);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorsSummary() {
            return String.join("\n", errors);
        }
    }

    /**
     * Valide toutes les règles de saisie pour un Merch
     * Équivalent des @Assert\NotBlank, @Assert\PositiveOrZero de Symfony
     */
    public static ValidationResult validate(
            String name,
            String priceText,
            String stockText,
            String type) {

        ValidationResult result = new ValidationResult();

        // ── name : @Assert\NotBlank ─────────────────────────────────────────
        if (name == null || name.isBlank()) {
            result.addError("Nom du produit", "Le nom du produit est obligatoire");
        } else if (name.length() > 255) {
            result.addError("Nom du produit", "Le nom ne doit pas dépasser 255 caractères");
        }

        // ── price : @Assert\NotBlank + @Assert\PositiveOrZero ───────────────
        if (priceText == null || priceText.isBlank()) {
            result.addError("Prix", "Le prix est obligatoire");
        } else {
            try {
                int price = Integer.parseInt(priceText.trim());
                if (price < 0) {
                    result.addError("Prix", "Le prix doit être positif ou zéro");
                }
            } catch (NumberFormatException e) {
                result.addError("Prix", "Le prix doit être un nombre entier valide");
            }
        }

        // ── stock : @Assert\NotBlank + @Assert\PositiveOrZero ───────────────
        if (stockText == null || stockText.isBlank()) {
            result.addError("Stock", "Le stock est obligatoire");
        } else {
            try {
                int stock = Integer.parseInt(stockText.trim());
                if (stock < 0) {
                    result.addError("Stock", "Le stock doit être positif ou zéro");
                }
            } catch (NumberFormatException e) {
                result.addError("Stock", "Le stock doit être un nombre entier valide");
            }
        }

        // ── type : @Assert\NotBlank ─────────────────────────────────────────
        if (type == null || type.isBlank()) {
            result.addError("Type de produit", "Le type de produit est obligatoire");
        }

        return result;
    }

    /**
     * Valide directement un objet Merch
     */
    public static ValidationResult validate(Merch merch) {
        return validate(
                merch.getName(),
                String.valueOf(merch.getPrice()),
                String.valueOf(merch.getStock()),
                merch.getType()
        );
    }
}
