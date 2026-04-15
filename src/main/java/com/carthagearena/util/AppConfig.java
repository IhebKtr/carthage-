package com.carthagearena.util;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

/**
 * Chargeur de configuration depuis le fichier .env
 * Équivalent de l'accès aux $_ENV de Symfony
 *
 * Usage :
 *   String apiKey = AppConfig.get("GROQ_API_KEY");
 */
public class AppConfig {

    private static final Dotenv dotenv;

    static {
        Dotenv loaded;
        try {
            loaded = Dotenv.configure()
                    .directory("./")        // cherche .env à la racine du projet
                    .ignoreIfMissing()      // ne plante pas si .env absent
                    .load();
        } catch (DotenvException e) {
            loaded = Dotenv.configure().ignoreIfMissing().load();
        }
        dotenv = loaded;
    }

    /**
     * Retourne la valeur d'une variable d'environnement.
     * Cherche d'abord dans le .env, puis dans les variables système.
     *
     * @throws IllegalStateException si la clé est manquante ou vide
     */
    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Variable de configuration manquante : " + key +
                    "\nVérifiez votre fichier .env à la racine du projet."
            );
        }
        return value;
    }

    /**
     * Retourne la valeur ou null si absente.
     */
    public static String get(String key) {
        // Priorité : .env > variable système
        String val = dotenv.get(key);
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        return val;
    }

    /**
     * Retourne la valeur ou une valeur par défaut.
     */
    public static String get(String key, String defaultValue) {
        String val = get(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }
}
