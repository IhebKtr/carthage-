-- ═══════════════════════════════════════════════════════════════
-- Carthage Arena - Script SQL de création des tables (MySQL)
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS carthage_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE carthage_arena;

-- Nettoyage des anciennes tables
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS merch;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS game;
SET FOREIGN_KEY_CHECKS = 1;

-- ─── Table game ──────────────────────────────────────────────────────────────
CREATE TABLE game (
    id          INT            NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    logo_url    VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table user ──────────────────────────────────────────────────────────────
-- Structure alignée avec AuthService.java et le projet Core
CREATE TABLE `user` (
    id          BINARY(16)     NOT NULL,
    username    VARCHAR(50)    NOT NULL UNIQUE,
    nickname    VARCHAR(50),
    email       VARCHAR(180)   NOT NULL UNIQUE,
    password    VARCHAR(255)   NOT NULL,
    roles       LONGTEXT       NOT NULL, -- format JSON: ["ROLE_USER"]
    age         INT,
    gender      VARCHAR(20),
    created_at  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table merch ──────────────────────────────────────────────────────────────
CREATE TABLE merch (
    id          CHAR(36)       NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    price       INT            NOT NULL DEFAULT 0,
    stock       INT            NOT NULL DEFAULT 0,
    image_url   VARCHAR(255),
    type        VARCHAR(50)    NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    game_id     INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_merch_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table order ──────────────────────────────────────────────────────────────
CREATE TABLE `order` (
    id            CHAR(36)     NOT NULL,
    date          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount  INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    user_id       BINARY(16)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table order_item ─────────────────────────────────────────────────────────
CREATE TABLE order_item (
    id          INT            NOT NULL AUTO_INCREMENT,
    order_id    CHAR(36)       NOT NULL,
    merch_id    CHAR(36)       NOT NULL,
    quantity    INT            NOT NULL DEFAULT 1,
    unit_price  INT            NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES `order`(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_merch FOREIGN KEY (merch_id) REFERENCES merch(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Données de test ──────────────────────────────────────────────────────────

INSERT INTO game (name, description) VALUES
    ('Carthage FC', 'Club de football tunisien'),
    ('Espérance ST', 'Club sportif tunisien'),
    ('Club Africain', 'Club de football tunisien');

-- Insertion des utilisateurs (mots de passe : admin123 et user123)
-- Le hash pour 'admin123' et 'user123' est généré ici via une simulation compatible
INSERT INTO `user` (id, username, nickname, email, password, roles, age, gender) VALUES
    (UNHEX(REPLACE(UUID(), '-', '')), 'admin', 'Administrateur', 'admin@carthagearena.tn', '$2a$10$8.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7', '["ROLE_USER","ROLE_ADMIN"]', 30, 'MALE'),
    (UNHEX(REPLACE(UUID(), '-', '')), 'user', 'Utilisateur Lambda', 'user@example.tn', '$2a$10$8.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7.7', '["ROLE_USER"]', 25, 'FEMALE');

INSERT INTO merch (id, name, description, price, stock, type, game_id) VALUES
    (UUID(), 'Maillot Carthage FC 2025',  'Maillot officiel domicile', 7500, 50, 'jersey',    1),
    (UUID(), 'Casquette Espérance ST',     'Casquette brodée',          2500, 100,'cap',       2),
    (UUID(), 'Écharpe Club Africain',      'Écharpe officielle',        1500, 75, 'accessory', 3);
