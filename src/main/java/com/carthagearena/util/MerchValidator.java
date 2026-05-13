package com.carthagearena.util;

import com.carthagearena.model.Merch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validateur de saisie pour l'entité Merch
 * Équivalent des Assert Symfony côté Java
 * Inclut des règles avancées : longueur min/max, format URL, limites de prix/stock
 */
public class MerchValidator {

    // ─── Constantes de validation ────────────────────────────────────────────
    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 2000;
    private static final int PRICE_MAX = 9999999; // 99999.99 DT
    private static final int STOCK_MAX = 999999;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://|data:image/).*", Pattern.CASE_INSENSITIVE);

    private static final List<String> VALID_TYPES = List.of(
            "shirt", "cap", "jersey", "poster", "accessory", "other"
    );

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
     * + règles avancées (longueur min, format URL, type valide, limites)
     */
    public static ValidationResult validate(
            String name,
            String priceText,
            String stockText,
            String type) {

        ValidationResult result = new ValidationResult();

        // ── name : @Assert\NotBlank + longueur min/max ──────────────────────
        if (name == null || name.isBlank()) {
            result.addError("Nom du produit", "Le nom du produit est obligatoire");
        } else if (name.trim().length() < NAME_MIN_LENGTH) {
            result.addError("Nom du produit", "Le nom doit contenir au moins " + NAME_MIN_LENGTH + " caractères");
        } else if (name.trim().length() > NAME_MAX_LENGTH) {
            result.addError("Nom du produit", "Le nom ne doit pas dépasser " + NAME_MAX_LENGTH + " caractères");
        }

        // ── price : @Assert\NotBlank + @Assert\PositiveOrZero + limite ──────
        if (priceText == null || priceText.isBlank()) {
            result.addError("Prix", "Le prix est obligatoire");
        } else {
            try {
                int price = Integer.parseInt(priceText.trim());
                if (price < 0) {
                    result.addError("Prix", "Le prix doit être positif ou zéro");
                } else if (price > PRICE_MAX) {
                    result.addError("Prix", "Le prix ne peut pas dépasser " + String.format("%.2f DT", PRICE_MAX / 100.0));
                }
            } catch (NumberFormatException e) {
                result.addError("Prix", "Le prix doit être un nombre entier valide (en centimes)");
            }
        }

        // ── stock : @Assert\NotBlank + @Assert\PositiveOrZero + limite ──────
        if (stockText == null || stockText.isBlank()) {
            result.addError("Stock", "Le stock est obligatoire");
        } else {
            try {
                int stock = Integer.parseInt(stockText.trim());
                if (stock < 0) {
                    result.addError("Stock", "Le stock doit être positif ou zéro");
                } else if (stock > STOCK_MAX) {
                    result.addError("Stock", "Le stock ne peut pas dépasser " + STOCK_MAX);
                }
            } catch (NumberFormatException e) {
                result.addError("Stock", "Le stock doit être un nombre entier valide");
            }
        }

        // ── type : @Assert\NotBlank + valeur valide ─────────────────────────
        if (type == null || type.isBlank()) {
            result.addError("Type de produit", "Le type de produit est obligatoire");
        } else if (!VALID_TYPES.contains(type.toLowerCase())) {
            result.addError("Type de produit", "Type invalide. Valeurs autorisées : " + String.join(", ", VALID_TYPES));
        }

        return result;
    }

    /**
     * Valide l'URL d'une image (optionnel, mais si rempli doit être valide)
     */
    public static ValidationResult validateImageUrl(String imageUrl) {
        ValidationResult result = new ValidationResult();
        if (imageUrl != null && !imageUrl.isBlank()) {
            if (!URL_PATTERN.matcher(imageUrl.trim()).matches()) {
                result.addError("Image", "L'URL de l'image doit commencer par http:// , https:// ou data:image/");
            }
        }
        return result;
    }

    /**
     * Validation complète avec l'image
     */
    public static ValidationResult validateFull(
            String name, String priceText, String stockText,
            String type, String imageUrl) {

        ValidationResult result = validate(name, priceText, stockText, type);

        // Ajouter la validation image
        ValidationResult imageResult = validateImageUrl(imageUrl);
        for (String error : imageResult.getErrors()) {
            result.getErrors().add(error);
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

    /**
     * Vérifie si une description est valide (longueur max)
     */
    public static boolean isDescriptionValid(String description) {
        return description == null || description.length() <= DESCRIPTION_MAX_LENGTH;
    }

    /**
     * Retourne les types valides pour l'UI
     */
    public static List<String> getValidTypes() {
        return VALID_TYPES;
    }
}
