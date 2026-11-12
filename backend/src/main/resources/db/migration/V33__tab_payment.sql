-- Pagamento adiantado: a mesa pede a conta com itens ainda a caminho, paga, e a comanda continua
-- aberta. Cada pagamento recebido fica registrado; no fechamento, o que faltava vira o último.
CREATE TABLE tab_payment (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    tab_id           UUID        NOT NULL REFERENCES tab (id),
    amount_cents     BIGINT      NOT NULL CHECK (amount_cents > 0),
    payment_method   VARCHAR(10) NOT NULL CHECK (payment_method IN ('PIX', 'CARD', 'CASH')),
    received_at      TIMESTAMPTZ NOT NULL,
    received_by      UUID        NOT NULL REFERENCES app_user (id),
    -- O pedido de conta atendido ao receber; NULL no pagamento do fechamento.
    call_id          UUID REFERENCES call (id)
);
CREATE INDEX tab_payment_establishment_id_idx ON tab_payment (establishment_id);
CREATE INDEX tab_payment_tab_id_idx ON tab_payment (tab_id, received_at);
CREATE INDEX tab_payment_received_by_idx ON tab_payment (received_by);
CREATE INDEX tab_payment_call_id_idx ON tab_payment (call_id);

-- Fechada sem nada a cobrar (tudo cancelado) não tem forma de pagamento: a comanda fechada exige só
-- o total. As comandas já fechadas têm os dois e continuam valendo.
ALTER TABLE tab DROP CONSTRAINT tab_closed_has_total;
ALTER TABLE tab ADD CONSTRAINT tab_closed_has_total CHECK ((status = 'CLOSED') = (total_cents IS NOT NULL));
