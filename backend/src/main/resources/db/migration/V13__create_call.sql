-- Chamados (Guia 13): a mesa chama o garçom ou pede a conta; a equipe aceita e conclui.
CREATE TABLE call (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    table_id         UUID        NOT NULL REFERENCES dining_table (id),
    session_id       UUID        NOT NULL REFERENCES table_session (id),
    type             VARCHAR(10) NOT NULL CHECK (type IN ('WAITER', 'BILL')),
    -- Só o pedido de conta tem forma de pagamento (e ele sempre tem).
    payment_method   VARCHAR(10) CHECK (payment_method IN ('PIX', 'CARD', 'CASH')),
    status           VARCHAR(15) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE', 'CANCELED')),
    created_at       TIMESTAMPTZ NOT NULL,
    accepted_at      TIMESTAMPTZ,
    accepted_by      UUID REFERENCES app_user (id),
    done_at          TIMESTAMPTZ,
    canceled_at      TIMESTAMPTZ,
    CONSTRAINT call_payment_only_for_bill CHECK ((type = 'BILL') = (payment_method IS NOT NULL))
);
CREATE INDEX call_establishment_id_idx ON call (establishment_id);
CREATE INDEX call_table_id_idx ON call (table_id);
CREATE INDEX call_session_id_idx ON call (session_id);
CREATE INDEX call_accepted_by_idx ON call (accepted_by);
-- Uma mesa tem no máximo um chamado ABERTO de cada tipo (especificação): tocar de novo não duplica.
CREATE UNIQUE INDEX call_one_open_per_type_uq ON call (table_id, type) WHERE status IN ('PENDING', 'IN_PROGRESS');
-- A fila da equipe: os abertos do restaurante, por ordem de chegada.
CREATE INDEX call_queue_idx ON call (establishment_id, created_at) WHERE status IN ('PENDING', 'IN_PROGRESS');
