-- Refresh tokens do login (Guia 3). Guarda só o SHA-256 do token, nunca o valor.
-- family_id agrupa os tokens de uma mesma sessão (o id do primeiro token).

CREATE TABLE refresh_token (
                               id               UUID        PRIMARY KEY,
                               family_id        UUID        NOT NULL,
                               user_id          UUID        NOT NULL REFERENCES app_user (id),
                               establishment_id UUID        NOT NULL REFERENCES establishment (id),
                               token_hash       VARCHAR(64) NOT NULL,
                               created_at       TIMESTAMPTZ NOT NULL,
                               expires_at       TIMESTAMPTZ NOT NULL,
                               revoked_at       TIMESTAMPTZ,
                               CONSTRAINT refresh_token_hash_uq UNIQUE (token_hash),
                               CONSTRAINT refresh_token_expiry_ck CHECK (expires_at > created_at)
);
CREATE INDEX refresh_token_family_id_idx ON refresh_token (family_id);
CREATE INDEX refresh_token_user_id_idx ON refresh_token (user_id);
CREATE INDEX refresh_token_establishment_id_idx ON refresh_token (establishment_id);
