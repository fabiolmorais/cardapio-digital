-- Recuperação de senha (Guia 33): o link que vai por e-mail. Só o hash do token fica no banco
-- (quem lê o banco não consegue redefinir a senha de ninguém), e o link vale uma vez só.
-- A pessoa é global (como em app_user): a tabela não tem establishment_id.
CREATE TABLE password_reset_token (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_user (id),
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    CONSTRAINT password_reset_token_hash_uq UNIQUE (token_hash)
);

-- A chave estrangeira, e a conta dos pedidos da última hora de cada pessoa.
CREATE INDEX password_reset_token_user_created_idx ON password_reset_token (user_id, created_at);
