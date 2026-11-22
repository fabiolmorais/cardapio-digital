-- Dados novos dos relatórios (R5, plano dos relatórios, seção 6): começam a ser gravados antes das
-- telas que os usam, porque o histórico só existe a partir do dia em que se começa a guardar.

-- Turnos da equipe (REL-53), pela entrada com o PIN: abre no login do PIN e fecha quando a pessoa
-- sai do app, outra pessoa entra no mesmo aparelho, o aparelho é desvinculado ou a sessão expira.
-- family_id: a sessão (família de refresh tokens) aberta naquela entrada.
CREATE TABLE staff_shift (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    user_id          UUID        NOT NULL REFERENCES app_user (id),
    device_id        UUID        NOT NULL REFERENCES device (id),
    family_id        UUID        NOT NULL UNIQUE,
    started_at       TIMESTAMPTZ NOT NULL,
    ended_at         TIMESTAMPTZ,
    end_reason       VARCHAR(10) CHECK (end_reason IN ('LOGOUT', 'REPLACED', 'UNLINKED', 'EXPIRED')),
    CONSTRAINT staff_shift_end_pair CHECK ((ended_at IS NULL) = (end_reason IS NULL)),
    CONSTRAINT staff_shift_order CHECK (ended_at IS NULL OR ended_at >= started_at)
);
CREATE INDEX staff_shift_establishment_idx ON staff_shift (establishment_id, started_at);
CREATE INDEX staff_shift_user_id_idx ON staff_shift (user_id);
CREATE INDEX staff_shift_device_open_idx ON staff_shift (device_id) WHERE ended_at IS NULL;

-- Histórico do produto (REL-22, 24, 25): cada vez que a promoção, o destaque ou o "disponível"
-- mudam, uma linha com o estado novo. promo_price_cents: o preço da promoção que entrou.
CREATE TABLE product_status_change (
    id                UUID        PRIMARY KEY,
    establishment_id  UUID        NOT NULL REFERENCES establishment (id),
    product_id        UUID        NOT NULL REFERENCES product (id),
    kind              VARCHAR(10) NOT NULL CHECK (kind IN ('PROMOTION', 'FEATURED', 'AVAILABLE')),
    active            BOOLEAN     NOT NULL,
    promo_price_cents BIGINT      CHECK (promo_price_cents >= 0),
    changed_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX product_status_change_establishment_idx ON product_status_change (establishment_id, changed_at);
CREATE INDEX product_status_change_product_id_idx ON product_status_change (product_id, changed_at);

-- O ponto de partida: como cada produto ativo está hoje.
INSERT INTO product_status_change (id, establishment_id, product_id, kind, active, promo_price_cents, changed_at)
SELECT gen_random_uuid(), p.establishment_id, p.id, k.kind,
       CASE k.kind WHEN 'PROMOTION' THEN p.promo_price_cents IS NOT NULL
                   WHEN 'FEATURED' THEN p.featured
                   ELSE p.available END,
       CASE WHEN k.kind = 'PROMOTION' THEN p.promo_price_cents END,
       now()
FROM product p
CROSS JOIN (VALUES ('PROMOTION'), ('FEATURED'), ('AVAILABLE')) AS k (kind)
WHERE p.active;

-- Viradas do cartão da passagem (REL-74): cada vez que a mesa vira o cartão (verde ou vermelho).
-- O cartão começa verde na ativação (tab_rodizio.started_at).
CREATE TABLE rodizio_card_change (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    tab_rodizio_id   UUID        NOT NULL REFERENCES tab_rodizio (id),
    status           VARCHAR(10) NOT NULL CHECK (status IN ('SERVING', 'PAUSED')),
    changed_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX rodizio_card_change_establishment_idx ON rodizio_card_change (establishment_id, changed_at);
CREATE INDEX rodizio_card_change_tab_rodizio_id_idx ON rodizio_card_change (tab_rodizio_id);

-- Avisos de pedido repetido ao garçom (REL-50, ADR 0011): um por produto avisado. confirmed_at:
-- o garçom confirmou que era outro pedido; sem ele, o garçom tirou o item (ou desistiu do envio).
-- Gravado numa transação à parte, porque o envio avisado é recusado; tab_id sem chave estrangeira
-- de propósito, para essa transação não esperar a trava da comanda que o envio segura.
CREATE TABLE duplicate_warning (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    tab_id           UUID        NOT NULL,
    product_id       UUID        NOT NULL REFERENCES product (id),
    warned_by        UUID        NOT NULL REFERENCES app_user (id),
    warned_at        TIMESTAMPTZ NOT NULL,
    confirmed_at     TIMESTAMPTZ
);
CREATE INDEX duplicate_warning_establishment_idx ON duplicate_warning (establishment_id, warned_at);
CREATE INDEX duplicate_warning_tab_idx ON duplicate_warning (tab_id, product_id);
CREATE INDEX duplicate_warning_product_id_idx ON duplicate_warning (product_id);
CREATE INDEX duplicate_warning_warned_by_idx ON duplicate_warning (warned_by);

-- Pessoas na mesa fora do rodízio (REL-04), opcional, informado pelo garçom.
ALTER TABLE tab
    ADD COLUMN people INTEGER CHECK (people BETWEEN 1 AND 50);
