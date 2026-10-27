-- Aparelhos (Guia 5): celular do salão, tablet da cozinha e TV, autorizados por um restaurante.
-- token_hash fica vazio até o aparelho buscar o token (o banco só guarda o SHA-256).
CREATE TABLE device (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    name             VARCHAR(60) NOT NULL,
    type             VARCHAR(20) NOT NULL CHECK (type IN ('WAITER', 'KITCHEN', 'TV')),
    token_hash       VARCHAR(64),
    created_at       TIMESTAMPTZ NOT NULL,
    last_seen_at     TIMESTAMPTZ,
    revoked_at       TIMESTAMPTZ,
    CONSTRAINT device_token_hash_uq UNIQUE (token_hash)
);
CREATE INDEX device_establishment_id_idx ON device (establishment_id);

-- Pedido de vínculo: o aparelho novo mostra o código; o gerente digita no painel.
-- Ainda não pertence a nenhum restaurante: só passa a ter um quando aponta para o aparelho.
CREATE TABLE device_pairing (
    id          UUID        PRIMARY KEY,
    code        VARCHAR(6)  NOT NULL,
    secret_hash VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    device_id   UUID        REFERENCES device (id),
    claimed_at  TIMESTAMPTZ,
    CONSTRAINT device_pairing_expiry_ck CHECK (expires_at > created_at)
);
CREATE INDEX device_pairing_code_idx ON device_pairing (code);
CREATE INDEX device_pairing_device_id_idx ON device_pairing (device_id);
