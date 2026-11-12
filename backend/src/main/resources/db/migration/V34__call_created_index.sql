-- A espera média de hoje no "Ao vivo" e no Modo TV (desenhos 7.1 e 8.1) lê os chamados do dia,
-- abertos ou não: o índice da fila (call_queue_idx) só guarda os abertos.
CREATE INDEX call_created_idx ON call (establishment_id, created_at);
