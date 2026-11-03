-- O histórico de comandas no admin (Guia 21): as fechadas do restaurante, pela data de fechamento.
-- O índice parcial guarda só as fechadas (as abertas já têm o tab_open_idx da V18).
CREATE INDEX tab_closed_idx ON tab (establishment_id, closed_at) WHERE status = 'CLOSED';
