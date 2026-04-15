-- ═══════════════════════════════════════════════════════════════
-- Carthage Arena - Script SQL de création des tables (MySQL)
-- Traduit depuis les entités Symfony vers la structure Java
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS carthage_arena CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE carthage_arena;

-- ─── Table game ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS game (
    id          INT            NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    logo_url    VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table utilisateur ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS utilisateur (
    id          INT            NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(255)   NOT NULL,
    prenom      VARCHAR(255),
    email       VARCHAR(255)   NOT NULL UNIQUE,
    password    VARCHAR(255)   NOT NULL,
    role        VARCHAR(50)    DEFAULT 'ROLE_USER',
    created_at  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table merch ──────────────────────────────────────────────────────────────
-- Traduit depuis l'entité Symfony Merch.php
-- UUID stocké en CHAR(36) au lieu d'un type natif UUID
CREATE TABLE IF NOT EXISTS merch (
    id          CHAR(36)       NOT NULL,           -- UUID (équiv. UuidType Symfony)
    name        VARCHAR(255)   NOT NULL,           -- @Assert\NotBlank
    description TEXT,                              -- nullable: true
    price       INT            NOT NULL DEFAULT 0, -- @Assert\PositiveOrZero (centimes)
    stock       INT            NOT NULL DEFAULT 0, -- @Assert\PositiveOrZero
    image_url   VARCHAR(255),                      -- nullable: true
    type        VARCHAR(50)    NOT NULL,           -- @Assert\NotBlank
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    game_id     INT,                               -- @ManyToOne -> Game
    PRIMARY KEY (id),
    CONSTRAINT fk_merch_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table order ──────────────────────────────────────────────────────────────
-- Entité Order (regroupement d'achats)
-- Pourquoi ? Un utilisateur peut acheter plusieurs produits en une seule fois
CREATE TABLE IF NOT EXISTS `order` (
    id            CHAR(36)     NOT NULL,           -- UUID
    date          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount  INT          NOT NULL DEFAULT 0, -- en centimes
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | PAID | CANCELLED
    user_id       INT          NOT NULL,           -- référence utilisateur
    PRIMARY KEY (id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Table order_item ─────────────────────────────────────────────────────────
-- Lignes de commande : quantité + prix snapshot (au moment de l'achat)
CREATE TABLE IF NOT EXISTS order_item (
    id          INT            NOT NULL AUTO_INCREMENT,
    order_id    CHAR(36)       NOT NULL,
    merch_id    CHAR(36)       NOT NULL,
    quantity    INT            NOT NULL DEFAULT 1,
    unit_price  INT            NOT NULL,           -- snapshot du prix au moment de l'achat
    PRIMARY KEY (id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES `order`(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_merch FOREIGN KEY (merch_id) REFERENCES merch(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Données de test ──────────────────────────────────────────────────────────

INSERT INTO game (name, description) VALUES
    ('Carthage FC', 'Club de football tunisien'),
    ('Espérance ST', 'Club sportif tunisien'),
    ('Club Africain', 'Club de football tunisien');

INSERT INTO utilisateur (nom, prenom, email, password, role) VALUES
    ('Admin', 'Super', 'admin@carthagearena.tn', 'hashed_password', 'ROLE_ADMIN'),
    ('Ben Ali', 'Ahmed', 'ahmed@example.tn', 'hashed_password', 'ROLE_USER'),
    ('Trabelsi', 'Sonia', 'sonia@example.tn', 'hashed_password', 'ROLE_USER');

INSERT INTO merch (id, name, description, price, stock, type, game_id) VALUES
    (UUID(), 'Maillot Carthage FC 2025',  'Maillot officiel domicile', 7500, 50, 'jersey',    1),
    (UUID(), 'Casquette Espérance ST',     'Casquette brodée',          2500, 100,'cap',       2),
    (UUID(), 'Écharpe Club Africain',      'Écharpe officielle',        1500, 75, 'accessory', 3),
    (UUID(), 'Poster Stade de Rades',      'Poster A2 haute qualité',   800,  200,'poster',    NULL),
    (UUID(), 'T-shirt Carthage Arena',     'T-shirt coton premium',     3500, 30, 'shirt',     1);

-- Exemple de commande
SET @order_id = UUID();
INSERT INTO `order` (id, total_amount, status, user_id)
    VALUES (@order_id, 10000, 'PENDING', 2);
INSERT INTO order_item (order_id, merch_id, quantity, unit_price)
    SELECT @order_id, id, 1, price FROM merch WHERE name = 'Maillot Carthage FC 2025' LIMIT 1;
INSERT INTO order_item (order_id, merch_id, quantity, unit_price)
    SELECT @order_id, id, 1, price FROM merch WHERE name = 'Casquette Espérance ST' LIMIT 1;
