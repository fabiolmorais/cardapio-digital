-- Relatório de equipe (R9): "da conta pedida ao fechamento" busca, para cada pedido de conta, a
-- primeira comanda fechada da mesa depois dele. Sem este índice, a busca lia todas as comandas da
-- mesa a cada chamado (cerca de 10 s em 12 meses de dados).
CREATE INDEX tab_table_closed_idx ON tab (table_id, closed_at) WHERE status = 'CLOSED';
