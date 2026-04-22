package com.carthage.services;

public class PasswordResetServiceTest {

  public static void main(String[] args) {
    // ⚠️ REMPLACE par l'email du compte qui existe dans ta DB
    String realEmail = "jelassi.aymen94@gmail.com";
    String fakeEmail = "inexistant.zzz.12345@example.com";

    PasswordResetService service = new PasswordResetService();

    System.out.println("=== Test 1 : requestReset avec email RÉEL ===");
    System.out.println("Email : " + realEmail);
    System.out.println("Début de la demande...");
    long start = System.currentTimeMillis();
    try {
      service.requestReset(realEmail);
      long duration = System.currentTimeMillis() - start;
      System.out.println("✅ Demande exécutée en " + duration + " ms");
      System.out.println("📬 Vérifie ta boîte mail : tu devrais recevoir un code à 6 chiffres");
      System.out.println("🗄️  Vérifie en DB : SELECT * FROM password_reset_token ORDER BY created_at DESC LIMIT 1;");
    } catch (PasswordResetService.ResetException e) {
      System.err.println("❌ ÉCHEC : " + e.getMessage());
      e.printStackTrace();
    }

    System.out.println("\n=== Test 2 : Anti-enumeration (email inconnu) ===");
    System.out.println("Email : " + fakeEmail);
    try {
      service.requestReset(fakeEmail);
      System.out.println("✅ Aucune exception lancée (comportement attendu : silence)");
      System.out.println("   → Un attaquant ne peut pas distinguer ce cas du cas 'email valide'");
    } catch (PasswordResetService.ResetException e) {
      System.err.println("❌ ÉCHEC anti-enumeration : " + e.getMessage());
      System.err.println("   → Le service a révélé que cet email n'existe pas. Fuite de sécurité.");
    }

    System.out.println("\n=== Test 3 : Validation format invalide ===");
    try {
      service.requestReset("pas-un-email");
      System.err.println("❌ ÉCHEC : aurait dû throw pour format invalide");
    } catch (PasswordResetService.ResetException e) {
      System.out.println("✅ Exception attrapée comme attendu : " + e.getMessage());
    }

    System.out.println("\n=== Test 4 : Validation email vide ===");
    try {
      service.requestReset("");
      System.err.println("❌ ÉCHEC : aurait dû throw pour email vide");
    } catch (PasswordResetService.ResetException e) {
      System.out.println("✅ Exception attrapée comme attendu : " + e.getMessage());
    }

    System.out.println("\n=== Fin des tests ===");
  }
}