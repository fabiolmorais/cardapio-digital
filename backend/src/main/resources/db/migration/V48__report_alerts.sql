-- Alertas em tempo real dos relatórios (REL-78, R13): por push no celular do dono e dos gerentes.

-- A configuração do restaurante. Sem linha, vale o padrão: cozinha (25 min) e chamado (3 min)
-- ligados, comanda aberta (6 h) desligada.
CREATE TABLE report_alert_setting (
    establishment_id UUID        PRIMARY KEY REFERENCES establishment (id),
    kitchen_enabled  BOOLEAN     NOT NULL,
    kitchen_minutes  INTEGER     NOT NULL CHECK (kitchen_minutes BETWEEN 5 AND 120),
    call_enabled     BOOLEAN     NOT NULL,
    call_minutes     INTEGER     NOT NULL CHECK (call_minutes BETWEEN 1 AND 30),
    tab_enabled      BOOLEAN     NOT NULL,
    tab_hours        INTEGER     NOT NULL CHECK (tab_hours BETWEEN 1 AND 24),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- O último alerta de cada tipo: no máximo um a cada 15 min, e só pelo que passou do limite depois
-- dele. A marcação vem antes do envio (com várias instâncias, só uma envia).
CREATE TABLE report_alert_sent (
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    alert_type       VARCHAR(10) NOT NULL CHECK (alert_type IN ('KITCHEN', 'CALL', 'TAB')),
    sent_at          TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (establishment_id, alert_type)
);
