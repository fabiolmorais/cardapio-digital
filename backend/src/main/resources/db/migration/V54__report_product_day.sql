-- Resumo diário por produto para os relatórios de períodos longos (ADR 0014).
-- Cada linha é um dia do restaurante (no fuso dele), um setor e um produto. Os vendidos ficam no dia
-- em que a comanda fechou e no setor da mesa da comanda; os preparos, no dia em que o item foi pedido
-- e no setor da mesa do pedido (as mesmas regras das consultas diretas).
CREATE TABLE report_product_day (
    establishment_id   UUID    NOT NULL REFERENCES establishment (id),
    day                DATE    NOT NULL,
    sector_id          UUID    NOT NULL REFERENCES sector (id),
    product_id         UUID    NOT NULL REFERENCES product (id),
    sold_quantity      INTEGER NOT NULL DEFAULT 0 CHECK (sold_quantity >= 0),
    sold_revenue_cents BIGINT  NOT NULL DEFAULT 0 CHECK (sold_revenue_cents >= 0),
    preparations       INTEGER NOT NULL DEFAULT 0 CHECK (preparations >= 0),
    prep_millis        BIGINT  NOT NULL DEFAULT 0 CHECK (prep_millis >= 0),
    PRIMARY KEY (establishment_id, day, sector_id, product_id)
);
CREATE INDEX report_product_day_sector_id_idx ON report_product_day (sector_id);
CREATE INDEX report_product_day_product_id_idx ON report_product_day (product_id);

-- Até onde o resumo vai: os dias antes de summarized_until estão na tabela acima. Sem linha aqui, o
-- restaurante ainda não tem resumo, e os relatórios leem tudo direto.
CREATE TABLE report_summary_state (
    establishment_id UUID        PRIMARY KEY REFERENCES establishment (id),
    summarized_until DATE        NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
