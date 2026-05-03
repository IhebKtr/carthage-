# Presentation Live - Module User & Auth (Discord OAuth2)

## 1) Objectif du travail

L'objectif etait d'ameliorer l'authentification du module **User & Auth** en ajoutant une connexion sociale avec **Discord OAuth2**, tout en gardant le login classique email/mot de passe.

Resultat: l'utilisateur peut maintenant se connecter avec Discord depuis:
- la page d'inscription
- la page de connexion

## 2) Ce que j'ai implemente concretement

### A. Service OAuth2 Discord dedie

J'ai cree un service `DiscordOAuthService` qui gere tout le cycle OAuth2:
- construction de l'URL d'autorisation (`client_id`, `redirect_uri`, `scope`, `state`)
- ouverture du navigateur systeme
- reception du callback sur une URL locale (loopback localhost)
- verification du `state` (protection CSRF)
- echange du `code` contre un `access_token`
- recuperation du profil Discord (`id`, `email`, `username/global_name`, `verified`)

### B. Liaison avec les comptes locaux

J'ai etendu `UserService` avec la logique metier suivante:
- si `discord_id` existe deja -> connexion directe
- sinon, recherche par email
  - email existant -> liaison automatique du `discord_id`
  - email absent -> creation automatique d'un nouveau compte local

Cette logique permet d'eviter les doublons et d'offrir une UX fluide.

### C. Integration UI/Controller

- `SignupController`: le bouton Discord n'est plus un placeholder, il lance le vrai flow OAuth.
- `LoginController`: ajout du meme flow OAuth + bouton Discord dans la vue login.
- En cas de succes: creation de session (`SessionContext`) puis redirection selon le role.

## 3) Securite et bonnes pratiques

Voici les points securite a mettre en avant pendant l'oral:
- utilisation du **Authorization Code Flow** (plus propre qu'un flow implicite)
- validation du parametre `state` (anti-CSRF)
- callback limite a `localhost`/`127.0.0.1` avec port explicite
- scopes minimaux: `identify email`
- gestion d'erreurs propre (consent refuse, timeout, callback invalide, probleme reseau)
- secrets non hardcodes (variables d'environnement via `.env`)

## 4) Variables d'environnement utilisees

- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `DISCORD_REDIRECT_URI`

Important: la valeur de `DISCORD_REDIRECT_URI` doit etre **exactement** la meme que celle enregistree dans le portail Discord Developer.

## 5) Comment demontrer en live (plan de demo)

Ordre recommande pour la presentation:

1. Montrer rapidement la page Sign Up avec bouton Discord.
2. Cliquer Discord -> montrer l'ouverture du navigateur.
3. Accepter le consentement Discord.
4. Revenir a l'app -> montrer la connexion automatique et la redirection dashboard.
5. Ouvrir la page Sign In -> refaire le test Discord pour montrer que ca marche aussi en login.
6. Expliquer la logique de liaison automatique par email.

## 6) Scenarios de test a citer au professeur

Tests fonctionnels valides:
- nouvel utilisateur Discord -> creation auto + login
- utilisateur local existant (meme email Discord) -> liaison auto + login
- utilisateur deja lie a Discord -> login direct
- consentement refuse -> message d'erreur utilisateur
- callback invalide/state invalide -> connexion bloquee de facon securisee

Tests de non-regression:
- login email/mot de passe fonctionne toujours
- signup classique fonctionne toujours
- flow mot de passe oublie/reset non impacte

## 7) Ce qu'il faut retenir (version courte)

Si tu dois resumer en 30 secondes:

"J'ai integre Discord OAuth2 dans mon module User & Auth avec un vrai Authorization Code Flow. J'ai ajoute un service dedie pour gerer l'auth, la securite (`state`, callback local, secrets en env), puis j'ai relie ca a mon `UserService` pour connecter, lier ou creer automatiquement les comptes. Enfin, j'ai branche le flow sur Sign Up et Sign In, avec verification de compilation/tests et sans casser les flows existants."

## 8) Script oral pret a dire demain

"Pour mon module User & Auth, j'ai choisi d'ajouter une authentification sociale avec Discord OAuth2.  
Techniquement, j'ai cree un service `DiscordOAuthService` qui gere tout le cycle OAuth: URL d'autorisation, ouverture navigateur, callback local, verification du state, echange de code, et recuperation du profil Discord.  
Ensuite, dans la couche metier, j'ai ajoute une logique de resolution de compte: si le Discord est deja lie on connecte directement, sinon on cherche par email, puis on lie ou on cree le compte automatiquement.  
J'ai integre ce flow dans les deux parcours utilisateur: inscription et connexion.  
Niveau securite, j'ai applique les bonnes pratiques: state anti-CSRF, scope minimal, callback localhost, gestion d'erreurs, et secrets externalises dans le `.env`.  
Le resultat est une experience utilisateur plus moderne, tout en gardant la compatibilite avec le login classique et les autres flows auth deja existants." 

## 9) Questions probables du professeur + reponses

### Q1: Pourquoi utiliser OAuth2 au lieu de stocker un mot de passe Discord?
Parce qu'on ne doit jamais manipuler les credentials Discord de l'utilisateur. OAuth2 delegue l'authentification au fournisseur officiel.

### Q2: A quoi sert le parametre `state`?
Il protege contre les attaques CSRF pendant le callback OAuth.

### Q3: Pourquoi callback localhost?
Parce que l'application est desktop (JavaFX): le navigateur revient sur un endpoint local temporaire.

### Q4: Que se passe-t-il si l'utilisateur refuse le consentement?
Le flow est interrompu proprement et un message d'erreur clair est affiche.

### Q5: Est-ce que ca casse votre login classique?
Non. Le login email/mot de passe reste intact et fonctionnel.
