# Documentation Technique - Fonctionnalités Avancées Carthage Arena

Ce document détaille l'implémentation et la logique métier de cinq fonctionnalités majeures de la plateforme Carthage Arena.

## 1. Fonctionnalité de Mail Reminder (Service d'emails automatiques)
**Fichier clé :** `EmailService.java`

Cette fonctionnalité est conçue pour envoyer automatiquement des rappels de matchs aux capitaines des équipes.

*   **Planificateur (Scheduler) :** Le système utilise un `ScheduledExecutorService` qui tourne en arrière-plan. Lorsqu'il est démarré (`startScheduler`), il s'exécute automatiquement toutes les heures sans bloquer l'interface utilisateur.
*   **Logique de ciblage (SQL) :** À chaque exécution, la méthode `checkAndSendReminders(24)` effectue une requête SQL complexe. Elle croise les tables `match_game`, `team`, `team_membership` et `user` pour trouver tous les matchs dont le statut est `SCHEDULED` et qui vont se jouer dans les prochaines **24 heures**. Elle cible spécifiquement les utilisateurs ayant le rôle `CAPTAIN`.
*   **Format de l'email :** Les e-mails sont envoyés via l'API `javax.mail` avec un serveur SMTP Gmail. Le contenu de l'e-mail est formaté en HTML personnalisé pour s'adapter au thème sombre et rouge de Carthage Arena, donnant un aspect professionnel aux communications.

---

## 2. Intégration de l'Intelligence Artificielle (Grok/Gemini Chat)
**Fichier clé :** `GrokChatController.java`

Cette fonctionnalité offre aux utilisateurs un assistant intelligent intégré à la plateforme, propulsé par l'API **Gemini-Flash**.

*   **Bridage (System Prompt) :** L'une des parties les plus intéressantes est le comportement imposé à l'IA. Au lieu d'avoir un chatbot générique, un "System Prompt" strict indique à l'IA qu'elle est l'assistant officiel de Carthage Arena. Elle a pour consigne de **refuser** toute question qui ne concerne pas les tournois, l'esport, ou le fonctionnement de la plateforme en renvoyant un message standard.
*   **Mémoire contextuelle :** Le code maintient une liste `history` contenant les 6 derniers échanges (12 messages : l'utilisateur et l'assistant). À chaque nouvelle question, tout cet historique est envoyé au format JSON à l'API, ce qui permet à l'IA de comprendre le contexte et de se souvenir des questions précédentes.
*   **Interface dynamique :** Les appels réseau étant lents, ils sont encapsulés dans un `CompletableFuture.supplyAsync` pour ne pas figer l'interface. Pendant que l'IA réfléchit, une animation de frappe (générant "...") rassure l'utilisateur.

---

## 3. Smart Calendar (Intégration Calendrier Intelligent)
**Fichier clé :** `ICalUtils.java`

Permet aux joueurs de synchroniser les matchs avec leur calendrier personnel (Google Calendar, Outlook, Apple Calendar).

*   **Génération `.ics` :** Le code ne dépend pas d'une API externe pour le calendrier, il génère manuellement le standard iCalendar. Il utilise le `FileChooser` de JavaFX pour laisser l'utilisateur choisir où sauvegarder le fichier.
*   **Structure du fichier :** Il écrit les balises officielles de formatage comme `BEGIN:VCALENDAR`, `DTSTART` (Date de début), `DTEND` (Date de fin générée automatiquement en rajoutant +2 heures), ainsi qu'un `UID` unique pour éviter que les calendriers ne confondent les événements. Les dates sont strictement parsées au format `yyyyMMdd'T'HHmmss`.

---

## 4. Match Making & Algorithme de modification des horaires
**Fichiers clés :** `MatchService.java` & `TournoiMatchesController.java`

*   **Génération du Bracket (Match Making) :** Selon le type de tournoi, l'algorithme se divise en deux :
    *   *Round Robin (Championnat)* : Deux boucles imbriquées croisent toutes les équipes pour que chacune joue contre toutes les autres.
    *   *Élimination directe* : L'algorithme mélange la liste des équipes aléatoirement (`Collections.shuffle(seeded)`), puis les regroupe par paires pour créer le premier tour.
    *   Le temps des matchs est incrémenté automatiquement (ex: +90 min) à l'aide de `scheduled = scheduled.plusMinutes(...)`.
*   **L'Algorithme d'échange (Swap Drag & Drop) :** Plutôt que de forcer l'administrateur à taper de nouvelles dates manuellement, un système Drag & Drop a été implémenté :
    1. L'admin "attrape" un match (`setOnDragDetected`). L'UUID de ce match est copié dans le presse-papiers (`Dragboard`).
    2. L'admin le "dépose" sur un autre match (`setOnDragDropped`).
    3. L'algorithme récupère l'objet match déplacé et l'objet ciblé. Il utilise une variable temporaire (`tempDate`) pour inverser les propriétés `ScheduledAt` des deux matchs en mémoire.
    4. Il exécute deux requêtes SQL `UPDATE` via `matchService.updateMatchDate(...)` pour sauvegarder l'inversion en base de données et recharge la vue instantanément.

---

## 5. La Map pour les détails des tournois
**Fichier clé :** `TournoiDetailController.java`

La carte utilise la librairie **Gluon Maps**, offrant une approche native et performante par rapport à une Webview classique.

*   **Mapping GPS :** La méthode `updateMap(String place)` contient une logique (`switch/case`) qui traduit automatiquement les noms des lieux de compétition (Sousse, Sfax, Monastir, Bizerte, Esprit) en coordonnées GPS exactes (latitude / longitude).
*   **Création du point de la carte :** L'objet `MapPoint` centre la carte sur ces coordonnées, puis applique un zoom par défaut (zoom de 13).
*   **Couche de dessin personnalisée :** Pour marquer l'endroit de façon personnalisée, le code utilise une classe locale `CustomMapLayer` qui hérite de la classe `MapLayer` de Gluon. Elle convertit les coordonnées GPS en pixels via la méthode `getMapPoint()`, puis dessine et positionne un point de repère (un cercle rouge avec un bord blanc) correspondant à la charte graphique de Carthage Arena.
