package com.carthage.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {

    private final Dotenv dotenv;

    private EnvConfig() {
        this.dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
    }

    private static class Holder {
        private static final EnvConfig INSTANCE = new EnvConfig();
    }

    public static EnvConfig getInstance() {
        return Holder.INSTANCE;
    }

    public String get(String key) {
        return dotenv.get(key);
    }

    public String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Variable d'environnement manquante : " + key +
                            ". Vérifie ton fichier .env à la racine du projet.");
        }
        return value;
    }

    public int getIntRequired(String key) {
        String raw = getRequired(key);
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "La variable " + key + " doit être un entier, reçu : " + raw);
        }
    }
}