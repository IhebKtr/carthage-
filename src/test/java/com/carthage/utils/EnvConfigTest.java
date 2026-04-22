package com.carthage.utils;

public class EnvConfigTest {

  public static void main(String[] args) {
    System.out.println("=== Test 1 : Lecture des variables d'environnement ===");
    EnvConfig config = EnvConfig.getInstance();

    System.out.println("MAIL_HOST : " + config.getRequired("MAIL_HOST"));
    System.out.println("MAIL_PORT : " + config.getIntRequired("MAIL_PORT"));
    System.out.println("MAIL_USERNAME : " + config.getRequired("MAIL_USERNAME"));
    System.out.println("MAIL_FROM_NAME : " + config.getRequired("MAIL_FROM_NAME"));
    System.out.println("MAIL_PASSWORD :  ****** Masqué pour sécurité");

    System.out.println("=== Test 2 : Singleton (même instance) ===");
    EnvConfig config2 = EnvConfig.getInstance();
    System.out.println("config == config2 : " + (config == config2));

    System.out.println("\n=== Tous les tests sont passés ===");
  }

}
