-- Os pedidos esperando a confirmação do garçom (Guia 19), do mais antigo ao mais novo. O índice
-- parcial só guarda os parados: é uma lista curta, mesmo com o histórico inteiro na tabela.
CREATE INDEX customer_order_waiting_idx ON customer_order (establishment_id, created_at)
    WHERE status = 'PENDING_APPROVAL';
