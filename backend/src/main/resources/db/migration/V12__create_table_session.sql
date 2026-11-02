-- Sessão da mesa (Guia 12): a visita de um celular a uma mesa, presa a um cookie.
-- A trava de presença (Etapa 7) acrescenta como a presença foi provada, numa migração nova.
CREATE TABLE table_session (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    table_id         UUID        NOT NULL REFERENCES dining_table (id),
    -- Só o hash do token do cookie, como o refresh token (Guia 3): quem copiar a tabela não usa as sessões.
    token_hash       VARCHAR(64) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    last_seen_at     TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    ended_at         TIMESTAMPTZ,
    CONSTRAINT table_session_token_hash_uq UNIQUE (token_hash)
);
CREATE INDEX table_session_establishment_id_idx ON table_session (establishment_id);
CREATE INDEX table_session_table_id_idx ON table_session (table_id);
