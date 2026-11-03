-- Fechar a comanda (Guia 20): o total e a forma de pagamento ficam gravados nela. O total é
-- guardado (e não recalculado) porque a comanda fechada é um registro: é o que foi cobrado.
ALTER TABLE tab ADD COLUMN total_cents BIGINT CHECK (total_cents >= 0);
ALTER TABLE tab ADD COLUMN payment_method VARCHAR(10) CHECK (payment_method IN ('PIX', 'CARD', 'CASH'));
-- Fechada ⇔ tem total e forma de pagamento (aberta, nenhum dos dois).
ALTER TABLE tab ADD CONSTRAINT tab_closed_has_total
    CHECK ((status = 'CLOSED') = (total_cents IS NOT NULL AND payment_method IS NOT NULL));
-- As comandas abertas do restaurante, para o painel: o índice parcial guarda só as abertas.
CREATE INDEX tab_open_idx ON tab (establishment_id, opened_at) WHERE status IN ('OPEN', 'CLOSING');
