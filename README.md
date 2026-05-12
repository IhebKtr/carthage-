# 🏟️ Carthage Arena

Plateforme esports de gestion de tournois développée en Java avec JavaFX.

## 📋 Description

**Carthage Arena** est une application desktop permettant aux joueurs de créer des comptes, former des équipes, participer à des tournois et dépenser de la monnaie virtuelle (Carthage Points — CP) dans une boutique. Les arbitres officient les matchs et les administrateurs gèrent l'ensemble du système (utilisateurs, tournois, jeux, réclamations, boutique).

## 🛠️ Stack Technique

- **Langage** : Java 17
- **Framework UI** : JavaFX 17 (Controls, FXML, Web)
- **Build** : Maven
- **Base de données** : MySQL 8 (via JDBC `mysql-connector-j`)
- **Sécurité** : jBCrypt (hash des mots de passe), TOTP + ZXing (2FA / QR Code)
- **Mail** : JavaMail (SMTP)
- **PDF** : iTextPDF
- **Intégrations API** :
  - **Cloudinary** — hébergement d'images
  - **Stripe** — paiements
  - **Discord OAuth2** — authentification
  - **RAWG** — catalogue de jeux
  - **Skinport** — boutique de skins
- **Configuration** : `dotenv-java` (fichier `.env`)
- **Vocal** : Vosk (reconnaissance vocale)
- **Cartes** : Gluon Maps
- **Identifiants** : UUID

## ⚙️ Prérequis

- Java JDK 17 ou supérieur
- Maven 3.8+ (ou utiliser le wrapper `./mvnw`)
- MySQL 8.x
- Un IDE compatible JavaFX (IntelliJ IDEA recommandé)

## 🚀 Installation

1. **Cloner le dépôt**
   ```bash
   git clone https://github.com/IhebKtr/carthage-.git
   cd carthage-
   ```

2. **Installer les dépendances**
   ```bash
   ./mvnw clean install
   ```
   Sous Windows :
   ```bash
   mvnw.cmd clean install
   ```

3. **Configurer la base de données**

   Créer une base MySQL nommée `carthage_arena` (ou autre, à adapter dans `DatabaseConnection.java`) :
   ```sql
   CREATE DATABASE carthage_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

4. **Configurer l'environnement**

   Copier le fichier d'exemple et le compléter :
   ```bash
   cp .env.example .env
   ```

   Variables à renseigner dans `.env` :
   ```env
   # Mail (SMTP Gmail)
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=votre-email@gmail.com
   MAIL_PASSWORD=votre-mot-de-passe-application
   MAIL_FROM_NAME=Carthage Arena

   # Discord OAuth
   DISCORD_CLIENT_ID=...
   DISCORD_CLIENT_SECRET=...
   DISCORD_REDIRECT_URI=http://localhost:51723/callback

   # Cloudinary
   CLOUDINARY_CLOUD_NAME=...
   CLOUDINARY_API_KEY=...
   CLOUDINARY_API_SECRET=...

   # Stripe
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_PUBLIC_KEY=pk_test_...

   # RAWG (catalogue de jeux)
   RAWG_API_KEY=...
   ```

5. **Lancer l'application**
   ```bash
   ./mvnw javafx:run
   ```
   Ou directement depuis l'IDE en exécutant la classe `com.carthage.MainApp`.

## 📁 Structure du Projet

```
src/main/java/com/carthage/
├── CarthageApplication.java       # Point d'entrée principal
├── MainApp.java                   # Application JavaFX
├── controllers/
│   ├── admin/                     # Contrôleurs admin (Tournois, Jeux, Réclamations)
│   └── user/                      # Contrôleurs utilisateur (Login, Dashboard, Boutique, Équipe...)
├── entity/                        # Entités métier (User, Team, Tournoi, Match, Skin...)
│   └── enums/                     # Énumérations (Statuts, Rôles, Rareté, Catégories)
├── services/                      # Services métier (UserService, EmailService, ReclamationService, OAuth...)
└── utils/                         # Utilitaires (DatabaseConnection, EnvConfig, SessionContext, UUIDUtils)

src/main/resources/com/carthage/
├── css/main.css                   # Feuilles de style globales
└── view/
    ├── admin/                     # Vues FXML admin
    └── user/                      # Vues FXML utilisateur

src/test/java/                     # Tests JUnit 5
scratch/                           # Scripts de débogage et vérification de schéma
```

## 🎮 Fonctionnalités

### Joueurs
- Création de compte, connexion classique ou via **Discord OAuth2**
- Authentification à deux facteurs (**2FA TOTP** avec QR Code)
- Réinitialisation de mot de passe par email avec code
- Gestion du profil (bio, avatar via Cloudinary)
- Création et gestion d'équipes
- Système d'invitation et adhésion
- Inscription et participation aux tournois
- **Boutique** : achat de skins via **Stripe**, monnaie virtuelle (CP)
- Soumission et suivi de **réclamations**

### Arbitres
- Officiation des matchs
- Saisie des résultats

### Administrateurs
- Tableau de bord d'administration
- Gestion des utilisateurs, jeux, tournois, boutique
- Traitement des réclamations (réponses, statut, priorité)
- Création / édition de tournois

## 🔐 Rôles Utilisateur

| Rôle      | Description                                            |
|-----------|--------------------------------------------------------|
| `USER`    | Joueur — rôle par défaut                               |
| `REFEREE` | Arbitre — peut officier les matchs                     |
| `ADMIN`   | Administrateur — accès au panneau d'administration     |

## 🧪 Tests

```bash
# Exécuter tous les tests
./mvnw test

# Exécuter une classe de test spécifique
./mvnw test -Dtest=UserServiceUpdateProfileTest
```

Tests disponibles :
- `EmailServiceTest`
- `PasswordResetConfirmTest`
- `PasswordResetServiceTest`
- `RolesParsingTest`
- `UserServiceUpdateProfileTest`
- `EnvConfigTest`

## 📝 Commandes Utiles

```bash
# Compiler le projet
./mvnw clean compile

# Construire le JAR
./mvnw package

# Lancer l'application JavaFX
./mvnw javafx:run

# Générer les entités à partir de la base
python generate_entities.py
```

## 📚 Vues Principales

| Vue                | Fichier FXML                         | Description                              |
|--------------------|--------------------------------------|------------------------------------------|
| Connexion          | `view/user/login-view.fxml`          | Page de connexion                        |
| Inscription        | `view/user/signup-view.fxml`         | Création de compte                       |
| Mot de passe oublié| `view/user/forgot-password-view.fxml`| Récupération par email                   |
| Tableau de bord    | `view/user/dashboard-view.fxml`      | Accueil utilisateur                      |
| Profil             | `view/user/profil-view.fxml`         | Profil utilisateur                       |
| Équipe             | `view/user/equipe-view.fxml`         | Gestion d'équipe                         |
| Tournois           | `view/user/tournois-view.fxml`       | Liste des tournois                       |
| Boutique           | `view/user/boutique-view.fxml`       | Achat de skins                           |
| Réclamations       | `view/user/reclamations-view.fxml`   | Gestion des réclamations                 |
| Admin Tournois     | `view/admin/tournois-view.fxml`      | Administration des tournois              |
| Admin Jeux         | `view/admin/games-view.fxml`         | Administration des jeux                  |
| Admin Réclamations | `view/admin/reclamations-view.fxml`  | Traitement des réclamations              |

## 👥 Équipe de Développement

Projet développé en équipe dans le cadre d'un projet académique. Branches actives :
- `master` — Branche principale
- `Aymen` — Module utilisateurs / authentification / 2FA
- `Tournoi-Match` — Module tournois et matchs
- `bilel` — Module équipes
- `mohamed` — Intégrations API (Cloudinary, Stripe, RAWG)
- `shop` — Module boutique
- `traduction` — Internationalisation

## 📄 Licence

Projet académique — Tous droits réservés.
