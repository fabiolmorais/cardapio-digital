-- Pedido lançado pelo garçom (ADR 0011).

-- O restaurante decide se a equipe do salão pode lançar pedidos. Começa desligado.
ALTER TABLE order_settings
    ADD COLUMN staff_ordering_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- O pedido vem de um celular da mesa OU de uma pessoa da equipe, nunca dos dois nem de nenhum.
ALTER TABLE customer_order
    ALTER COLUMN session_id DROP NOT NULL,
    ADD COLUMN placed_by           UUID REFERENCES app_user (id),
    -- Gerado pela tela a cada envio: repetir por falha de rede não cria outro pedido.
    ADD COLUMN client_request_id   UUID,
    -- O garçom viu o aviso de pedido repetido e confirmou que é outro pedido.
    ADD COLUMN duplicate_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT customer_order_one_origin CHECK (num_nonnulls(session_id, placed_by) = 1);

CREATE INDEX customer_order_placed_by_idx ON customer_order (placed_by);
CREATE UNIQUE INDEX customer_order_client_request_uq ON customer_order (establishment_id, client_request_id)
    WHERE client_request_id IS NOT NULL;
