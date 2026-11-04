-- O rodízio na mesa (Guia 25).

-- "Quero o rodízio" é um chamado novo da mesa.
ALTER TABLE call DROP CONSTRAINT call_type_check;
ALTER TABLE call ADD CONSTRAINT call_type_check CHECK (type IN ('WAITER', 'BILL', 'UNLOCK', 'RODIZIO'));

-- O rodízio de uma comanda: no máximo um (tab_id é único). O nome é copiado, como o nome do item
-- no pedido: renomear o rodízio depois não muda a conta de quem já foi.
CREATE TABLE tab_rodizio (
    id               UUID         PRIMARY KEY,
    establishment_id UUID         NOT NULL REFERENCES establishment (id),
    tab_id           UUID         NOT NULL UNIQUE REFERENCES tab (id),
    rodizio_id       UUID         NOT NULL REFERENCES rodizio (id),
    rodizio_name     VARCHAR(60)  NOT NULL,
    activated_by     UUID         NOT NULL REFERENCES app_user (id),
    started_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX tab_rodizio_establishment_id_idx ON tab_rodizio (establishment_id);
CREATE INDEX tab_rodizio_rodizio_id_idx ON tab_rodizio (rodizio_id);
CREATE INDEX tab_rodizio_activated_by_idx ON tab_rodizio (activated_by);

-- As pessoas por faixa, com o preço COPIADO na ativação (o do dia e horário de então).
CREATE TABLE tab_rodizio_guest (
    tab_rodizio_id   UUID         NOT NULL REFERENCES tab_rodizio (id),
    position         INT          NOT NULL CHECK (position >= 0),
    establishment_id UUID         NOT NULL REFERENCES establishment (id),
    tier_name        VARCHAR(40)  NOT NULL,
    quantity         INT          NOT NULL CHECK (quantity BETWEEN 0 AND 99),
    unit_price_cents BIGINT       NOT NULL CHECK (unit_price_cents >= 0),
    PRIMARY KEY (tab_rodizio_id, position)
);
CREATE INDEX tab_rodizio_guest_establishment_id_idx ON tab_rodizio_guest (establishment_id);

-- O item pedido numa rodada: preço zero, ligado ao rodízio da comanda.
ALTER TABLE order_item ADD COLUMN tab_rodizio_id UUID REFERENCES tab_rodizio (id);
CREATE INDEX order_item_tab_rodizio_id_idx ON order_item (tab_rodizio_id);
