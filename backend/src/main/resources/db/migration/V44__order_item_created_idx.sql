-- Relatórios da cozinha (ADR 0013, decisão 7): os itens pedidos num período, por restaurante.
-- Sem ele, um dia do relatório varre todos os itens do restaurante desde a abertura.
CREATE INDEX order_item_created_idx ON order_item (establishment_id, created_at);
