-- A comanda impressa (para quem não tem tablet na cozinha): cada item guarda quando saiu na
-- impressora e quando saiu a via "CANCELADO", para nenhuma estação imprimir de novo nem perder um.
ALTER TABLE order_item
    ADD COLUMN printed_at        TIMESTAMPTZ,
    ADD COLUMN cancel_printed_at TIMESTAMPTZ;

-- O que falta imprimir: os itens a caminho ainda sem comanda.
CREATE INDEX order_item_print_pending_idx ON order_item (establishment_id, created_at)
    WHERE printed_at IS NULL AND status IN ('PENDING', 'PREPARING', 'READY');

-- Os cancelados depois de impressos, ainda sem a via "CANCELADO".
CREATE INDEX order_item_print_cancel_idx ON order_item (establishment_id, canceled_at)
    WHERE printed_at IS NOT NULL AND cancel_printed_at IS NULL AND status = 'CANCELED';

-- As comandas impressas nas últimas horas (para reimprimir).
CREATE INDEX order_item_printed_at_idx ON order_item (establishment_id, printed_at)
    WHERE printed_at IS NOT NULL;
