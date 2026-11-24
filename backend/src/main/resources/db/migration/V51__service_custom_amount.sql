-- Gorjeta de outro valor (RF39, complemento; etapa C4 do plano de continuação dos relatórios).

-- O restaurante decide se a mesa pode escolher outro valor no lugar do percentual. Começa desligado.
ALTER TABLE order_settings
    ADD COLUMN service_charge_custom_allowed BOOLEAN NOT NULL DEFAULT FALSE;

-- Na comanda: o outro valor escolhido (pela mesa ou pelo garçom), no lugar do percentual. Só existe
-- com o serviço aceito. O que foi cobrado continua em service_cents, como antes.
ALTER TABLE tab
    ADD COLUMN service_custom_cents BIGINT,
    ADD CONSTRAINT tab_service_custom CHECK (
        service_custom_cents IS NULL
        OR (service_accepted AND service_custom_cents BETWEEN 1 AND 100000));
