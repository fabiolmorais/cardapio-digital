-- O fim do teste grátis (Guia 31).

-- O plano que o dono escolheu na tela "Assinatura", à espera do pagamento. Os três campos vêm
-- juntos (ou nenhum): o script de ativação usa e limpa.
ALTER TABLE subscription
    ADD COLUMN requested_plan_id UUID REFERENCES plan (id),
    ADD COLUMN requested_cycle   VARCHAR(10) CHECK (requested_cycle IN ('MONTHLY', 'ANNUAL')),
    ADD COLUMN requested_at      TIMESTAMPTZ,
    ADD CONSTRAINT subscription_request_complete
        CHECK ((requested_plan_id IS NULL) = (requested_cycle IS NULL)
            AND (requested_plan_id IS NULL) = (requested_at IS NULL));

CREATE INDEX subscription_requested_plan_id_idx ON subscription (requested_plan_id);

-- Os avisos por e-mail do fim do teste que já saíram: um de cada tipo por assinatura. A chave
-- primária é o que impede o e-mail repetido (duas instâncias, ou a tarefa rodando de novo).
CREATE TABLE subscription_notice (
    subscription_id UUID        NOT NULL REFERENCES subscription (id),
    kind            VARCHAR(30) NOT NULL CHECK (kind IN ('ENDS_IN_3_DAYS', 'ENDS_TOMORROW', 'ENDED')),
    sent_at         TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (subscription_id, kind)
);

-- A tarefa dos avisos procura os testes que acabam nos próximos dias.
CREATE INDEX subscription_trialing_ends_idx ON subscription (trial_ends_at) WHERE status = 'TRIALING';
