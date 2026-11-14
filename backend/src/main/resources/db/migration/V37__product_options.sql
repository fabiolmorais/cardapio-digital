-- Opções dos produtos (ADR 0010): tamanhos do produto e grupos de opções reutilizáveis, que cobrem a
-- pizza meio a meio, o ponto da carne, os adicionais e os complementos. Produto sem nada disto
-- continua igual.
--
-- Tamanhos, grupos e opções nunca são apagados: os pedidos apontam para eles. Saem do cardápio por
-- desativação (active = false). As ligações (produto ↔ grupo, limites e preços por tamanho) são
-- trocadas inteiras a cada edição: ninguém aponta para elas.

CREATE TABLE product_variant (
    id                UUID        PRIMARY KEY,
    establishment_id  UUID        NOT NULL REFERENCES establishment (id),
    product_id        UUID        NOT NULL REFERENCES product (id),
    name              VARCHAR(30) NOT NULL,
    price_cents       BIGINT      NOT NULL CHECK (price_cents BETWEEN 0 AND 9999999),
    promo_price_cents BIGINT      CHECK (promo_price_cents > 0 AND promo_price_cents < price_cents),
    available         BOOLEAN     NOT NULL DEFAULT TRUE,
    rodizio_default   BOOLEAN     NOT NULL DEFAULT FALSE,
    position          INT         NOT NULL CHECK (position >= 0),
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX product_variant_establishment_id_idx ON product_variant (establishment_id);
CREATE INDEX product_variant_product_id_idx ON product_variant (product_id);

CREATE TABLE option_group (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    name             VARCHAR(60) NOT NULL,
    title            VARCHAR(60) NOT NULL,
    min_choices      INT         NOT NULL CHECK (min_choices >= 0),
    max_choices      INT         NOT NULL CHECK (max_choices BETWEEN 1 AND 20),
    max_per_option   INT         NOT NULL CHECK (max_per_option >= 1),
    splits_item      BOOLEAN     NOT NULL DEFAULT FALSE,
    pricing          VARCHAR(10) NOT NULL CHECK (pricing IN ('SUM', 'HIGHEST', 'AVERAGE')),
    free_quantity    INT         NOT NULL DEFAULT 0 CHECK (free_quantity >= 0),
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (min_choices <= max_choices),
    CHECK (max_per_option <= max_choices),
    CHECK (NOT splits_item OR (max_choices <= 4 AND max_per_option = 1)),
    CHECK (free_quantity = 0 OR pricing = 'SUM')
);
CREATE INDEX option_group_establishment_id_idx ON option_group (establishment_id);

CREATE TABLE option_item (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    option_group_id  UUID        NOT NULL REFERENCES option_group (id),
    name             VARCHAR(60) NOT NULL,
    price_cents      BIGINT      NOT NULL CHECK (price_cents BETWEEN 0 AND 9999999),
    available        BOOLEAN     NOT NULL DEFAULT TRUE,
    position         INT         NOT NULL CHECK (position >= 0),
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX option_item_establishment_id_idx ON option_item (establishment_id);
CREATE INDEX option_item_option_group_id_idx ON option_item (option_group_id);

-- Os grupos ligados a cada produto, na ordem em que o cliente vê.
CREATE TABLE product_option_group (
    id               UUID     PRIMARY KEY,
    establishment_id UUID     NOT NULL REFERENCES establishment (id),
    product_id       UUID     NOT NULL REFERENCES product (id),
    option_group_id  UUID     NOT NULL REFERENCES option_group (id),
    position         INT      NOT NULL CHECK (position >= 0),
    UNIQUE (product_id, option_group_id)
);
CREATE INDEX product_option_group_establishment_id_idx ON product_option_group (establishment_id);
CREATE INDEX product_option_group_option_group_id_idx ON product_option_group (option_group_id);

-- O mínimo e o máximo de um grupo num tamanho do produto (broto: 1 sabor; grande: até 3).
CREATE TABLE product_option_group_limit (
    id                 UUID     PRIMARY KEY,
    establishment_id   UUID     NOT NULL REFERENCES establishment (id),
    product_id         UUID     NOT NULL REFERENCES product (id),
    option_group_id    UUID     NOT NULL REFERENCES option_group (id),
    product_variant_id UUID     NOT NULL REFERENCES product_variant (id),
    min_choices        INT      NOT NULL CHECK (min_choices >= 0),
    max_choices        INT      NOT NULL CHECK (max_choices BETWEEN 1 AND 20),
    CHECK (min_choices <= max_choices),
    UNIQUE (product_id, option_group_id, product_variant_id)
);
CREATE INDEX product_option_group_limit_establishment_id_idx ON product_option_group_limit (establishment_id);
CREATE INDEX product_option_group_limit_option_group_id_idx ON product_option_group_limit (option_group_id);
CREATE INDEX product_option_group_limit_product_variant_id_idx ON product_option_group_limit (product_variant_id);

-- O preço de uma opção num tamanho (a calabresa custa mais na grande).
CREATE TABLE option_item_price (
    id                 UUID   PRIMARY KEY,
    establishment_id   UUID   NOT NULL REFERENCES establishment (id),
    product_id         UUID   NOT NULL REFERENCES product (id),
    option_item_id     UUID   NOT NULL REFERENCES option_item (id),
    product_variant_id UUID   NOT NULL REFERENCES product_variant (id),
    price_cents        BIGINT NOT NULL CHECK (price_cents BETWEEN 0 AND 9999999),
    UNIQUE (option_item_id, product_variant_id)
);
CREATE INDEX option_item_price_establishment_id_idx ON option_item_price (establishment_id);
CREATE INDEX option_item_price_product_id_idx ON option_item_price (product_id);
CREATE INDEX option_item_price_product_variant_id_idx ON option_item_price (product_variant_id);

-- O pedido copia o tamanho e cada escolha: mudar o cardápio depois não muda o que foi pedido.
ALTER TABLE order_item
    ADD COLUMN product_variant_id UUID REFERENCES product_variant (id),
    ADD COLUMN variant_name       VARCHAR(30);
CREATE INDEX order_item_product_variant_id_idx ON order_item (product_variant_id);

CREATE TABLE order_item_option (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    order_item_id    UUID        NOT NULL REFERENCES order_item (id),
    option_group_id  UUID        NOT NULL REFERENCES option_group (id),
    option_item_id   UUID        NOT NULL REFERENCES option_item (id),
    group_title      VARCHAR(60) NOT NULL,
    option_name      VARCHAR(60) NOT NULL,
    splits_item      BOOLEAN     NOT NULL,
    quantity         INT         NOT NULL CHECK (quantity >= 1),
    price_cents      BIGINT      NOT NULL CHECK (price_cents >= 0),
    charged_cents    BIGINT      NOT NULL CHECK (charged_cents >= 0),
    position         INT         NOT NULL CHECK (position >= 0)
);
CREATE INDEX order_item_option_establishment_id_idx ON order_item_option (establishment_id);
CREATE INDEX order_item_option_order_item_id_idx ON order_item_option (order_item_id);
CREATE INDEX order_item_option_option_group_id_idx ON order_item_option (option_group_id);
CREATE INDEX order_item_option_option_item_id_idx ON order_item_option (option_item_id);
