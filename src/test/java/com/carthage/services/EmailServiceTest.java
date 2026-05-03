package com.carthage.services;

public class EmailServiceTest {

    public static void main(String[] args) {
        // ⚠️ Remplace par TON PROPRE email pour recevoir le mail de test
        String destinataire = "jelassi.aymen94@gmail.com";

        System.out.println("=== Test d'envoi d'email ===");
        System.out.println("Destinataire : " + destinataire);
        System.out.println("Initialisation du service...");

        try {
            EmailService emailService = new EmailService();
            System.out.println("✅ EmailService initialisé (config chargée)");

            System.out.println("Envoi en cours (peut prendre quelques secondes)...");
            long start = System.currentTimeMillis();

            emailService.send(
                    destinataire,
                    "Test Carthage Arena",
                    "Bonjour !\n\n" +
                            "Ceci est un email de test envoyé depuis l'application Carthage Arena.\n" +
                            "Si tu lis ceci, ça veut dire que jakarta.mail + SMTP Gmail fonctionnent.\n\n" +
                            "À bientôt !\n" +
                            "— L'équipe Carthage");

            long duration = System.currentTimeMillis() - start;
            System.out.println("✅ Mail envoyé en " + duration + " ms");
            System.out.println("📬 Vérifie ta boîte à l'adresse : " + destinataire);
            System.out.println("   (Pense aussi à regarder dans Spam si rien n'apparaît)");

        } catch (EmailService.EmailException e) {
            System.err.println("❌ ÉCHEC de l'envoi :");
            System.err.println("   Message : " + e.getMessage());
            System.err.println("\n--- Stack trace complète pour diagnostic ---");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("❌ Configuration invalide :");
            System.err.println("   " + e.getMessage());
        }
    }
}