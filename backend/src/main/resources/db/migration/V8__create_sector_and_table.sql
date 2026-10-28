-- Salão (Guia 7): setores (Salão principal, Deck…) e mesas, cada mesa com o token do QR Code.

CREATE TABLE sector (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    name             VARCHAR(40) NOT NULL,
    position         INT         NOT NULL CHECK (position >= 0),
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX sector_establishment_id_idx ON sector (establishment_id);
-- Dois setores ativos não podem ter o mesmo nome no restaurante (sem diferenciar maiúsculas).
CREATE UNIQUE INDEX sector_name_uq ON sector (establishment_id, lower(name)) WHERE active;

-- "table" é palavra reservada no SQL; por isso dining_table (como na especificação).
CREATE TABLE dining_table (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    sector_id        UUID        NOT NULL REFERENCES sector (id),
    label            VARCHAR(10) NOT NULL,
    public_token     VARCHAR(64) NOT NULL,
    token_issued_at  TIMESTAMPTZ NOT NULL,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT dining_table_public_token_uq UNIQUE (public_token)
);
CREATE INDEX dining_table_establishment_id_idx ON dining_table (establishment_id);
CREATE INDEX dining_table_sector_id_idx ON dining_table (sector_id);
-- O rótulo é único no restaurante, mesmo entre mesas desativadas: a "Mesa 13" desativada volta
-- a ser ativada, em vez de nascer uma segunda "Mesa 13".
CREATE UNIQUE INDEX dining_table_label_uq ON dining_table (establishment_id, lower(label));
