-- Comandas no balcão (ADR 0015, RF45).

-- O balcão é um lugar do salão, como a mesa, mas aceita várias comandas abertas e não tem QR Code.
-- O nome do balcão ("Balcão do bar") é mais comprido que o rótulo de uma mesa ("04").
ALTER TABLE dining_table
    ADD COLUMN kind VARCHAR(10) NOT NULL DEFAULT 'TABLE' CHECK (kind IN ('TABLE', 'COUNTER')),
    ALTER COLUMN label TYPE VARCHAR(30);

-- A comanda do balcão: o nome do cliente (obrigatório no balcão) e, se a casa usa, o cartão.
-- O nome continua na comanda quando ela muda do balcão para uma mesa (at_counter vira false).
ALTER TABLE tab
    ADD COLUMN at_counter    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN customer_name VARCHAR(40),
    ADD COLUMN card_number   VARCHAR(10),
    ADD CONSTRAINT tab_counter_has_name CHECK (NOT at_counter OR customer_name IS NOT NULL);

-- "Uma comanda aberta por mesa" vale só fora do balcão.
DROP INDEX tab_one_open_per_table_uq;
CREATE UNIQUE INDEX tab_one_open_per_table_uq ON tab (table_id)
    WHERE status IN ('OPEN', 'CLOSING') AND NOT at_counter;

-- O mesmo cartão não fica em duas comandas abertas do restaurante.
CREATE UNIQUE INDEX tab_open_card_number_uq ON tab (establishment_id, lower(card_number))
    WHERE status IN ('OPEN', 'CLOSING') AND card_number IS NOT NULL;

-- As comandas abertas do balcão, por lugar (a lista do balcão).
CREATE INDEX tab_open_counter_idx ON tab (establishment_id, table_id)
    WHERE status IN ('OPEN', 'CLOSING') AND at_counter;
