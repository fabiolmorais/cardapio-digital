-- O pedido de liberação passa a ser de um celular, e não da mesa: cada um tem o seu código, que o
-- garçom confere olhando a tela do cliente. Assim quem pede de longe não pega carona na liberação
-- de quem está no salão.
ALTER TABLE call ADD COLUMN release_code VARCHAR(4) CHECK (release_code ~ '^[0-9]{4}$');

-- Os pedidos de liberação abertos agora não têm código: são cancelados, e o cliente pede de novo.
UPDATE call SET status = 'CANCELED', canceled_at = now()
 WHERE type = 'UNLOCK' AND status IN ('PENDING', 'IN_PROGRESS');

-- Um aberto por tipo por mesa continua valendo para os outros chamados; a liberação, um por celular.
DROP INDEX call_one_open_per_type_uq;
CREATE UNIQUE INDEX call_one_open_per_type_uq ON call (table_id, type)
    WHERE status IN ('PENDING', 'IN_PROGRESS') AND type <> 'UNLOCK';
CREATE UNIQUE INDEX call_one_open_unlock_per_session_uq ON call (session_id)
    WHERE status IN ('PENDING', 'IN_PROGRESS') AND type = 'UNLOCK';
