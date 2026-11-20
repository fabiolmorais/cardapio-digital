-- Taxa de serviço opcional, os 10% do garçom (RF39; especificação, "Taxa de serviço").

-- A configuração do restaurante: começa desligada, 10%, registrada para a caixinha da equipe.
ALTER TABLE order_settings
    ADD COLUMN service_charge_enabled     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN service_charge_percent     INTEGER     NOT NULL DEFAULT 10 CHECK (service_charge_percent BETWEEN 1 AND 20),
    ADD COLUMN service_charge_destination VARCHAR(10) NOT NULL DEFAULT 'TEAM'
        CHECK (service_charge_destination IN ('TEAM', 'WAITER'));

-- Na comanda: o percentual copiado e se a mesa aceitou (os dois juntos, ou nenhum: sem serviço).
-- No fechamento: quanto foi cobrado, para quem ficou e, se for do garçom, qual.
ALTER TABLE tab
    ADD COLUMN service_percent     INTEGER     CHECK (service_percent BETWEEN 1 AND 20),
    ADD COLUMN service_accepted    BOOLEAN,
    ADD COLUMN service_cents       BIGINT      CHECK (service_cents >= 0),
    ADD COLUMN service_destination VARCHAR(10) CHECK (service_destination IN ('TEAM', 'WAITER')),
    ADD COLUMN service_waiter_id   UUID REFERENCES app_user (id),
    ADD CONSTRAINT tab_service_pair CHECK ((service_percent IS NULL) = (service_accepted IS NULL)),
    ADD CONSTRAINT tab_service_waiter CHECK (service_waiter_id IS NULL OR service_destination = 'WAITER');
CREATE INDEX tab_service_waiter_id_idx ON tab (service_waiter_id);
