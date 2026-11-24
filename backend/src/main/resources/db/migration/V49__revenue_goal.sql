-- Meta de faturamento (REL-79, R14): uma por mês, em centavos. Sem linha, o mês não tem meta.
-- Aparece na Visão geral e no resumo diário (Business): quanto já foi, quanto falta e quanto por dia.
CREATE TABLE revenue_goal (
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    month            DATE        NOT NULL CHECK (EXTRACT(DAY FROM month) = 1),
    amount_cents     BIGINT      NOT NULL CHECK (amount_cents > 0),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (establishment_id, month)
);
