package com.carthage.services;

import java.util.Scanner;

public class PasswordResetConfirmTest {

  public static void main(String[] args) {
    // ⚠️ Adapte ces deux valeurs
    String email = "jelassi.aymen94@gmail.com";
    String newPassword = "Carthage2026!"; // ← NOTE CE MDP, tu en auras besoin pour te reconnecter

    PasswordResetService service = new PasswordResetService();

    // ─── PHASE 1 : demander un fresh code ───
    System.out.println("=== Phase 1 : Demande d'un code ===");
    try {
      service.requestReset(email);
      System.out.println("✅ Code envoyé à " + email);
    } catch (PasswordResetService.ResetException e) {
      System.err.println("❌ Impossible de demander un code : " + e.getMessage());
      return;
    }

    // ─── PHASE 2 : attendre le code saisi ───
    String code;
    try (Scanner scanner = new Scanner(System.in)) {
      System.out.println("\n📬 Ouvre ta boîte mail, trouve le code à 6 chiffres, tape-le ici puis Entrée :");
      System.out.print("Code : ");
      code = scanner.nextLine().trim();
    }

    if (code.isEmpty()) {
      System.err.println("❌ Code vide, abandon");
      return;
    }

    // ─── PHASE 3 : Test 1 — confirmation valide ───
    System.out.println("\n=== Test 1 : Confirmation avec le bon code ===");
    try {
      service.confirmReset(email, code, newPassword);
      System.out.println("✅ Mot de passe changé !");
      System.out.println("   Nouveau mot de passe : " + newPassword);
      System.out.println("   Vérifie en DB avec : SELECT password, HEX(id) FROM user WHERE email = '" + email + "';");
      System.out.println("   Et le token doit avoir used_at rempli maintenant.");
    } catch (PasswordResetService.ResetException e) {
      System.err.println("❌ ÉCHEC Test 1 : " + e.getMessage());
      e.printStackTrace();
      return;
    }

    // ─── PHASE 4 : Test 2 — réutilisation du même code ───
    System.out.println("\n=== Test 2 : Réutiliser le même code (doit refuser) ===");
    try {
      service.confirmReset(email, code, "AutreMdp2026");
      System.err.println("❌ ÉCHEC : aurait dû refuser un code déjà utilisé — FAILLE single-use");
    } catch (PasswordResetService.ResetException e) {
      System.out.println("✅ Exception attendue : " + e.getMessage());
    }

    // ─── PHASE 5 : Test 3 — code invalide ───
    System.out.println("\n=== Test 3 : Code totalement faux (doit refuser) ===");
    try {
      service.confirmReset(email, "000000", "xxxxxx");
      System.err.println("❌ ÉCHEC : aurait dû refuser un faux code");
    } catch (PasswordResetService.ResetException e) {
      System.out.println("✅ Exception attendue : " + e.getMessage());
    }

    // ─── PHASE 6 : Test 4 — mdp trop court ───
    System.out.println("\n=== Test 4 : Mot de passe trop court (doit refuser) ===");
    try {
      service.confirmReset(email, "123456", "abc");
      System.err.println("❌ ÉCHEC : aurait dû refuser mdp trop court");
    } catch (PasswordResetService.ResetException e) {
      System.out.println("✅ Exception attendue : " + e.getMessage());
    }

    System.out.println("\n=== Fin des tests ===");
    System.out.println("\n➡️  Essaye maintenant de te connecter avec le nouveau mdp : " + newPassword);
  }
}