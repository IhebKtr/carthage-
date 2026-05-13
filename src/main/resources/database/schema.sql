-- ═══════════════════════════════════════════════════════════════
-- Carthage Arena - Script SQL COMPLET (MySQL / AlwaysData)
-- Inclut : Tables Core (master) + Tables Merch/Order (bilel)
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS carthage_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE carthage_arena;

-- ═══════════════════════════════════════════════════════════════
-- PARTIE 1 : Tables Core (depuis master / Symfony)
-- ═══════════════════════════════════════════════════════════════

-- ─── Table user (Entité Core) ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user` (
    id                     BINARY(16)     NOT NULL,
    email                  VARCHAR(180)   NOT NULL UNIQUE,
    username               VARCHAR(50)    NOT NULL UNIQUE,
    nickname               VARCHAR(50),
    password               VARCHAR(255)   NOT NULL,
    roles                  LONGTEXT       NOT NULL,
    balance                INT            NOT NULL DEFAULT 0,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    age                    INT            DEFAULT 0,
    gender                 VARCHAR(20)    DEFAULT 'UNKNOWN',
    is_verified            TINYINT(1)     NOT NULL DEFAULT 0,
    discord_id             VARCHAR(64)    UNIQUE,
    two_factor_secret      VARCHAR(255),
    is_two_factor_enabled  TINYINT(1)     DEFAULT 0,
    created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table auth_token ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auth_token (
    id          BINARY(16)     NOT NULL,
    value       VARCHAR(255)   NOT NULL,
    expires_at  DATETIME       NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id     BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table password_reset_token ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_token (
    id          BINARY(16)     NOT NULL,
    token       VARCHAR(255)   NOT NULL,
    expires_at  DATETIME       NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at     DATETIME,
    user_id     BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table profile ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS profile (
    id          BINARY(16)     NOT NULL,
    bio         TEXT,
    avatar_url  VARCHAR(255),
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME,
    user_id     BINARY(16)     NOT NULL UNIQUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table license ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS license (
    id           BINARY(16)     NOT NULL,
    license_code VARCHAR(255)   NOT NULL,
    is_used      TINYINT(1)     NOT NULL DEFAULT 0,
    used_at      DATETIME,
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_to  BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_license_user FOREIGN KEY (assigned_to) REFERENCES `user`(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table game (Core - UUID) ───────────────────────────────────────────────
-- Note : La table game du master utilise des UUIDs
CREATE TABLE IF NOT EXISTS game (
    id          BINARY(16)     NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    type        VARCHAR(50)    DEFAULT 'FPS',
    status      VARCHAR(20)    DEFAULT 'ACTIVE',
    image_url   VARCHAR(255),
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table team ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS team (
    id          BINARY(16)     NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    tag         VARCHAR(10),
    description TEXT,
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    invite_code VARCHAR(255),
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    captain_id  BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_team_captain FOREIGN KEY (captain_id) REFERENCES `user`(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table team_membership ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS team_membership (
    id          BINARY(16)     NOT NULL,
    role        VARCHAR(20)    NOT NULL DEFAULT 'MEMBER',
    joined_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    team_id     BINARY(16)     NOT NULL,
    player_id   BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_membership_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_player FOREIGN KEY (player_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table tournoi ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tournoi (
    id              BINARY(16)     NOT NULL,
    nom             VARCHAR(255)   NOT NULL,
    date_debut      DATETIME,
    date_fin        DATETIME,
    nb_equipes_max  INT            NOT NULL DEFAULT 8,
    prize_pool      INT            NOT NULL DEFAULT 0,
    status          VARCHAR(30)    NOT NULL DEFAULT 'UPCOMING',
    type            VARCHAR(30)    NOT NULL DEFAULT 'SINGLE_ELIMINATION',
    place           VARCHAR(255),
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME,
    game_id         BINARY(16),
    winner_id       BINARY(16),
    referee_id      BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_tournoi_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL,
    CONSTRAINT fk_tournoi_winner FOREIGN KEY (winner_id) REFERENCES team(id) ON DELETE SET NULL,
    CONSTRAINT fk_tournoi_referee FOREIGN KEY (referee_id) REFERENCES `user`(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table tournoi_team (table de jointure N-N) ──────────────────────────────
CREATE TABLE IF NOT EXISTS tournoi_team (
    tournoi_id  BINARY(16)     NOT NULL,
    team_id     BINARY(16)     NOT NULL,
    PRIMARY KEY (tournoi_id, team_id),
    CONSTRAINT fk_tt_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table match_game ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS match_game (
    id            BINARY(16)     NOT NULL,
    round         INT            NOT NULL DEFAULT 1,
    status        VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    scheduled_at  DATETIME,
    started_at    DATETIME,
    completed_at  DATETIME,
    score         VARCHAR(50),
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME,
    tournoi_id    BINARY(16)     NOT NULL,
    team1_id      BINARY(16),
    team2_id      BINARY(16),
    winner_id     BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_match_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_team1 FOREIGN KEY (team1_id) REFERENCES team(id) ON DELETE SET NULL,
    CONSTRAINT fk_match_team2 FOREIGN KEY (team2_id) REFERENCES team(id) ON DELETE SET NULL,
    CONSTRAINT fk_match_winner FOREIGN KEY (winner_id) REFERENCES team(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table skin ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS skin (
    id              BINARY(16)     NOT NULL,
    name            VARCHAR(255)   NOT NULL,
    description     TEXT,
    image_url       VARCHAR(255),
    price           INT            NOT NULL DEFAULT 0,
    rarity          VARCHAR(20)    NOT NULL DEFAULT 'COMMON',
    skin_type       VARCHAR(20)    NOT NULL DEFAULT 'DIGITAL',
    stock           INT            NOT NULL DEFAULT 0,
    api_provider    VARCHAR(255),
    delivery_method VARCHAR(255),
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    game_id         BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_skin_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table user_skin ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_skin (
    id           BINARY(16)     NOT NULL,
    purchased_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status       VARCHAR(20)    DEFAULT 'ACTIVE',
    user_id      BINARY(16)     NOT NULL,
    skin_id      BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_userskin_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
    CONSTRAINT fk_userskin_skin FOREIGN KEY (skin_id) REFERENCES skin(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table product ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product (
    id           INT            NOT NULL AUTO_INCREMENT,
    product_type VARCHAR(50),
    name         VARCHAR(255)   NOT NULL,
    description  TEXT,
    price_points INT            NOT NULL DEFAULT 0,
    available    TINYINT(1)     NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table purchase ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchase (
    id            BINARY(16)     NOT NULL,
    quantity      INT            NOT NULL DEFAULT 1,
    total_price   INT            NOT NULL DEFAULT 0,
    purchase_date DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    merch_id      CHAR(36),
    user_id       BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_purchase_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table reclamation ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reclamation (
    id          BINARY(16)     NOT NULL,
    subject     VARCHAR(255)   NOT NULL,
    message     TEXT           NOT NULL,
    category    VARCHAR(30)    NOT NULL DEFAULT 'OTHER',
    priority    VARCHAR(20)    NOT NULL DEFAULT 'MEDIUM',
    status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME,
    author_id   BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reclamation_author FOREIGN KEY (author_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table reclamation_response ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reclamation_response (
    id                BINARY(16)     NOT NULL,
    message           TEXT           NOT NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_admin_response TINYINT(1)     NOT NULL DEFAULT 0,
    reclamation_id    BINARY(16)     NOT NULL,
    author_id         BINARY(16)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_response_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation(id) ON DELETE CASCADE,
    CONSTRAINT fk_response_author FOREIGN KEY (author_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table day_plan ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS day_plan (
    id          INT            NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255),
    description TEXT,
    plan_date   DATE,
    user_id     BINARY(16),
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_dayplan_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table messenger_messages (Symfony) ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS messenger_messages (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    body         LONGTEXT       NOT NULL,
    headers      LONGTEXT       NOT NULL,
    queue_name   VARCHAR(190)   NOT NULL,
    created_at   DATETIME       NOT NULL,
    available_at DATETIME       NOT NULL,
    delivered_at DATETIME,
    PRIMARY KEY (id),
    INDEX IDX_queue (queue_name),
    INDEX IDX_available (available_at),
    INDEX IDX_delivered (delivered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table doctrine_migration_versions (Symfony) ─────────────────────────────
CREATE TABLE IF NOT EXISTS doctrine_migration_versions (
    version        VARCHAR(191)   NOT NULL,
    executed_at    DATETIME,
    execution_time INT,
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- PARTIE 2 : Tables Merch/Order (branche bilel - E-commerce)
-- ═══════════════════════════════════════════════════════════════

-- ─── Table merch ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS merch (
    id          CHAR(36)       NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    price       INT            NOT NULL DEFAULT 0,
    stock       INT            NOT NULL DEFAULT 0,
    image_url   VARCHAR(255),
    type        VARCHAR(50)    NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    game_id     BINARY(16),
    PRIMARY KEY (id),
    CONSTRAINT fk_merch_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table order ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `order` (
    id            CHAR(36)     NOT NULL,
    date          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount  INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    user_id       BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table order_item ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_item (
    id          INT            NOT NULL AUTO_INCREMENT,
    order_id    CHAR(36)       NOT NULL,
    merch_id    CHAR(36)       NOT NULL,
    quantity    INT            NOT NULL DEFAULT 1,
    unit_price  INT            NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES `order`(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_merch FOREIGN KEY (merch_id) REFERENCES merch(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ═══════════════════════════════════════════════════════════════
-- PARTIE 3 : Données de test
-- ═══════════════════════════════════════════════════════════════

-- Utilisateurs (admin + user normal)
INSERT INTO `user` (id, username, nickname, email, password, roles, age, gender, balance) VALUES
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', '')), 'admin', 'Administrateur', 'admin@carthagearena.tn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '["ROLE_USER","ROLE_ADMIN"]', 30, 'MALE', 10000),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')), 'user', 'Utilisateur Lambda', 'user@example.tn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '["ROLE_USER"]', 25, 'FEMALE', 5000),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440003', '-', '')), 'ahmed', 'Ahmed Ben Ali', 'ahmed@example.tn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '["ROLE_USER"]', 32, 'MALE', 2000),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440004', '-', '')), 'sonia', 'Sonia Trabelsi', 'sonia@example.tn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '["ROLE_USER"]', 21, 'FEMALE', 3000),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440005', '-', '')), 'yassine', 'Yassine Brahim', 'yassine@example.tn', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '["ROLE_USER"]', 17, 'MALE', 1500);

-- Jeux
INSERT INTO game (id, name, description, type, status) VALUES
    (UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00001', '-', '')), 'Carthage FC', 'Club de football tunisien', 'SPORT', 'ACTIVE'),
    (UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00002', '-', '')), 'Espérance ST', 'Club sportif tunisien', 'SPORT', 'ACTIVE'),
    (UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00003', '-', '')), 'Club Africain', 'Club de football tunisien', 'SPORT', 'ACTIVE');

-- Merch (votre logique e-commerce)
INSERT INTO merch (id, name, description, price, stock, type, game_id) VALUES
    (UUID(), 'Maillot Carthage FC 2025',  'Maillot officiel domicile', 7500, 50, 'jersey',    UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00001', '-', ''))),
    (UUID(), 'Casquette Espérance ST',     'Casquette brodée',          2500, 100,'cap',       UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00002', '-', ''))),
    (UUID(), 'Écharpe Club Africain',      'Écharpe officielle',        1500, 75, 'accessory', UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00003', '-', ''))),
    (UUID(), 'Poster Stade de Rades',      'Poster A2 haute qualité',   800,  200,'poster',    NULL),
    (UUID(), 'T-shirt Carthage Arena',     'T-shirt coton premium',     3500, 30, 'shirt',     UNHEX(REPLACE('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a00001', '-', '')));

-- Commande exemple
SET @order_id = UUID();
INSERT INTO `order` (id, total_amount, status, user_id)
    VALUES (@order_id, 10000, 'PAID', UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')));
INSERT INTO order_item (order_id, merch_id, quantity, unit_price)
    SELECT @order_id, id, 1, price FROM merch WHERE name = 'Maillot Carthage FC 2025' LIMIT 1;
INSERT INTO order_item (order_id, merch_id, quantity, unit_price)
    SELECT @order_id, id, 1, price FROM merch WHERE name = 'Casquette Espérance ST' LIMIT 1;
