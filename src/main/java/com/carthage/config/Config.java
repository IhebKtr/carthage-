package com.carthage.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public class Config {

    private static Dotenv dotenv;

    static {
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        } catch (DotenvException e) {
            System.err.println("Warning: Could not load .env file. " + e.getMessage());
        }
    }

    public static String get(String key) {
        if (dotenv != null) {
            return dotenv.get(key);
        }
        return System.getenv(key);
    }
    
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
}
