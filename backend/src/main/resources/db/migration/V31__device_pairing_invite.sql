-- O convite por QR Code: o pedido nasce vinculado pelo gerente e vence mesmo depois de vinculado.
ALTER TABLE device_pairing ADD COLUMN invite BOOLEAN NOT NULL DEFAULT false;
