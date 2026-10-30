-- Cardápio (Guia 9): categorias e produtos. As fotos (product_media) entram no Guia 10.

CREATE TABLE menu_category (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    name             VARCHAR(40) NOT NULL,
    position         INT         NOT NULL CHECK (position >= 0),
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX menu_category_establishment_id_idx ON menu_category (establishment_id);
-- Duas categorias ativas não podem ter o mesmo nome no restaurante (como os setores).
CREATE UNIQUE INDEX menu_category_name_uq ON menu_category (establishment_id, lower(name)) WHERE active;

-- Dinheiro em centavos (BIGINT), nunca em ponto flutuante. Os CHECKs repetem as regras do
-- domínio: são a última barreira se um bug deixar passar um preço impossível.
CREATE TABLE product (
    id                UUID         PRIMARY KEY,
    establishment_id  UUID         NOT NULL REFERENCES establishment (id),
    category_id       UUID         NOT NULL REFERENCES menu_category (id),
    name              VARCHAR(80)  NOT NULL,
    description       VARCHAR(300) NOT NULL DEFAULT '',
    price_cents       BIGINT       NOT NULL CHECK (price_cents BETWEEN 0 AND 9999999),
    promo_price_cents BIGINT       CHECK (promo_price_cents > 0 AND promo_price_cents < price_cents),
    featured          BOOLEAN      NOT NULL DEFAULT FALSE,
    available         BOOLEAN      NOT NULL DEFAULT TRUE,
    position          INT          NOT NULL CHECK (position >= 0),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX product_establishment_id_idx ON product (establishment_id);
CREATE INDEX product_category_id_idx ON product (category_id);
