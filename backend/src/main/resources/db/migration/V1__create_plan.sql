-- Planos de assinatura (dados globais do sistema, sem establishment_id).
-- Preço nulo = sob consulta; limite nulo = ilimitado; history_days 0 = ilimitado.
CREATE TABLE plan (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(40)  NOT NULL UNIQUE,
    name                VARCHAR(80)  NOT NULL,
    monthly_price_cents BIGINT       CHECK (monthly_price_cents >= 0),
    annual_price_cents  BIGINT       CHECK (annual_price_cents >= 0),
    max_tables          INTEGER      CHECK (max_tables >= 0),
    max_users           INTEGER      CHECK (max_users >= 0),
    max_sectors         INTEGER      CHECK (max_sectors >= 0),
    history_days        INTEGER      NOT NULL CHECK (history_days >= 0),
    entitlements        JSONB        NOT NULL DEFAULT '[]'::jsonb,
    support_level       VARCHAR(40)  NOT NULL,
    display_order       INTEGER      NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT plan_price_pair CHECK ((monthly_price_cents IS NULL) = (annual_price_cents IS NULL))
);

CREATE INDEX plan_active_display_order_idx ON plan (active, display_order);

-- Planos aprovados em 21/10/2026 (especificação, seção "Nossos planos").
INSERT INTO plan (code, name, monthly_price_cents, annual_price_cents, max_tables, max_users, max_sectors,
                  history_days, entitlements, support_level, display_order)
VALUES
    ('essential', 'Essencial', 6990, 69900, 20, 5, 1, 90,
     '[]', 'EMAIL', 1),
    ('pro', 'Pro', 11990, 119900, 50, NULL, 3, 365,
     '["PRODUCT_VIDEOS","PREP_STATIONS","ROLES_AND_PERMISSIONS","ADVANCED_REPORTS"]', 'WHATSAPP', 2),
    ('business', 'Business', 21990, 219900, 100, NULL, NULL, 0,
     '["PRODUCT_VIDEOS","PREP_STATIONS","ROLES_AND_PERMISSIONS","ADVANCED_REPORTS","REPORT_EXPORT"]', 'PRIORITY_WHATSAPP', 3),
    ('enterprise', 'Enterprise', NULL, NULL, NULL, NULL, NULL, 0,
     '["PRODUCT_VIDEOS","PREP_STATIONS","ROLES_AND_PERMISSIONS","ADVANCED_REPORTS","REPORT_EXPORT","MULTI_UNIT"]', 'DEDICATED_MANAGER', 4);
