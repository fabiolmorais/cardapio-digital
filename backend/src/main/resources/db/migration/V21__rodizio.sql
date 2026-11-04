-- O cadastro do rodízio (Guia 24): o rodízio, as faixas de preço, os preços por dia e horário e os
-- produtos incluídos. A mesa no rodízio (tab_rodizio) entra no Guia 25.

CREATE TABLE rodizio (
    id                         UUID         PRIMARY KEY,
    establishment_id           UUID         NOT NULL REFERENCES establishment (id),
    name                       VARCHAR(60)  NOT NULL,
    type                       VARCHAR(12)  NOT NULL CHECK (type IN ('A_LA_MINUTE', 'PASSAGEM')),
    description                VARCHAR(280) NOT NULL DEFAULT '',
    -- As regras opcionais: NULL quer dizer "sem essa regra" (o padrão, segundo a especificação).
    max_per_item_per_round     INT CHECK (max_per_item_per_round BETWEEN 1 AND 50),
    max_pending_per_guest      INT CHECK (max_pending_per_guest BETWEEN 1 AND 100),
    min_round_interval_minutes INT CHECK (min_round_interval_minutes BETWEEN 1 AND 120),
    time_limit_minutes         INT CHECK (time_limit_minutes BETWEEN 15 AND 600),
    waste_notice               VARCHAR(200),
    active                     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                 TIMESTAMPTZ  NOT NULL
);
CREATE INDEX rodizio_establishment_id_idx ON rodizio (establishment_id);
-- Dois rodízios ativos com o mesmo nome confundiriam o garçom na hora de ativar.
CREATE UNIQUE INDEX rodizio_active_name_uq ON rodizio (establishment_id, lower(name)) WHERE active;

-- As faixas (Adulto, Criança 5 a 10…), cada uma com o preço padrão, que vale sempre que
-- nenhuma exceção de dia e horário vale.
CREATE TABLE rodizio_tier (
    id                  UUID        PRIMARY KEY,
    establishment_id    UUID        NOT NULL REFERENCES establishment (id),
    rodizio_id          UUID        NOT NULL REFERENCES rodizio (id),
    name                VARCHAR(40) NOT NULL,
    default_price_cents BIGINT      NOT NULL CHECK (default_price_cents >= 0),
    position            INT         NOT NULL CHECK (position >= 0),
    UNIQUE (rodizio_id, position)
);
CREATE INDEX rodizio_tier_establishment_id_idx ON rodizio_tier (establishment_id);
CREATE INDEX rodizio_tier_rodizio_id_idx ON rodizio_tier (rodizio_id);

-- As exceções do preço: nestes dias (bits: segunda = 1, terça = 2 … domingo = 64), neste
-- horário (do restaurante), a faixa custa outro valor.
CREATE TABLE rodizio_price (
    id               UUID     PRIMARY KEY,
    establishment_id UUID     NOT NULL REFERENCES establishment (id),
    tier_id          UUID     NOT NULL REFERENCES rodizio_tier (id),
    days             SMALLINT NOT NULL CHECK (days BETWEEN 1 AND 127),
    starts_at        TIME     NOT NULL,
    ends_at          TIME     NOT NULL,
    price_cents      BIGINT   NOT NULL CHECK (price_cents >= 0),
    position         INT      NOT NULL CHECK (position >= 0),
    CHECK (starts_at < ends_at)
);
CREATE INDEX rodizio_price_establishment_id_idx ON rodizio_price (establishment_id);
CREATE INDEX rodizio_price_tier_id_idx ON rodizio_price (tier_id);

-- Os produtos incluídos (a aba Rodízio da mesa, no Guia 25).
CREATE TABLE rodizio_product (
    rodizio_id       UUID NOT NULL REFERENCES rodizio (id),
    product_id       UUID NOT NULL REFERENCES product (id),
    establishment_id UUID NOT NULL REFERENCES establishment (id),
    PRIMARY KEY (rodizio_id, product_id)
);
CREATE INDEX rodizio_product_product_id_idx ON rodizio_product (product_id);
CREATE INDEX rodizio_product_establishment_id_idx ON rodizio_product (establishment_id);
