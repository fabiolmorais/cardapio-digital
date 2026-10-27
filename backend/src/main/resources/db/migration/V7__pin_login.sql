-- Login por PIN (Guia 6).

-- Tentativas erradas de PIN por pessoa: 5 erros bloqueiam por 15 minutos.
-- Uma linha por vínculo; só existe depois do primeiro erro.
CREATE TABLE pin_lockout (
    membership_id    UUID        PRIMARY KEY REFERENCES membership (id),
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    failed_attempts  INT         NOT NULL CHECK (failed_attempts >= 0),
    locked_until     TIMESTAMPTZ
);
CREATE INDEX pin_lockout_establishment_id_idx ON pin_lockout (establishment_id);

-- Sessões abertas por PIN num aparelho: desvincular o aparelho encerra todas elas.
-- family_id é a família de refresh tokens da sessão (Guia 3).
CREATE TABLE device_session (
    family_id        UUID        PRIMARY KEY,
    device_id        UUID        NOT NULL REFERENCES device (id),
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    created_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX device_session_device_id_idx ON device_session (device_id);
CREATE INDEX device_session_establishment_id_idx ON device_session (establishment_id);
