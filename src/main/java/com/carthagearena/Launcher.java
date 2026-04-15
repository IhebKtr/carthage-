package com.carthagearena;

/**
 * Cette classe sert uniquement à contourner l'erreur de lancement JavaFX dans IntelliJ IDEA :
 * "Des composants d'exécution JavaFX obligatoires pour exécuter cette application sont manquants"
 * 
 * Lancez cette classe au lieu de MainApp !
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
