-- A assinatura da empresa (Etapa 14): o plano, a situação gravada e até quando está coberta.
-- Fica na organization (e não no establishment) porque o Enterprise com várias unidades paga
-- uma assinatura só. A situação de agora (teste, paga, tolerância, suspensa) é calculada com a
-- data, a partir de trial_ends_at e current_period_end.
CREATE TABLE subscription (
    id                 UUID         PRIMARY KEY,
    organization_id    UUID         NOT NULL REFERENCES organization (id),
    plan_id            UUID         NOT NULL REFERENCES plan (id),
    status             VARCHAR(20)  NOT NULL CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELED')),
    billing_cycle      VARCHAR(10)  CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    trial_ends_at      TIMESTAMPTZ  NOT NULL,
    current_period_end TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL,
    -- Paga (ou com a cobrança atrasada) sempre sabe até quando o período vai.
    CONSTRAINT subscription_paid_has_period
        CHECK (status NOT IN ('ACTIVE', 'PAST_DUE') OR current_period_end IS NOT NULL)
);

-- Uma assinatura por empresa (o índice único também serve à busca pela empresa).
CREATE UNIQUE INDEX subscription_organization_id_uq ON subscription (organization_id);
CREATE INDEX subscription_plan_id_idx ON subscription (plan_id);

-- Quem já tinha conta ganha o mesmo teste grátis de quem se cadastra agora: 14 dias de Pro.
INSERT INTO subscription (id, organization_id, plan_id, status, trial_ends_at, created_at)
SELECT gen_random_uuid(), o.id, p.id, 'TRIALING', now() + INTERVAL '14 days', now()
FROM organization o
CROSS JOIN plan p
WHERE p.code = 'pro';
