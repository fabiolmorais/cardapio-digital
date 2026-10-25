-- Contas: organização (quem assina), estabelecimento (o restaurante), usuário e vínculo com papel.
-- "user" é palavra reservada no PostgreSQL; por isso a tabela se chama app_user.

CREATE TABLE organization (
                              id          UUID         PRIMARY KEY,
                              name        VARCHAR(120) NOT NULL,
                              document    VARCHAR(14)  NOT NULL,
                              whatsapp    VARCHAR(11)  NOT NULL,
                              email       VARCHAR(254) NOT NULL,
                              created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE establishment (
                               id              UUID         PRIMARY KEY,
                               organization_id UUID         NOT NULL REFERENCES organization (id),
                               name            VARCHAR(120) NOT NULL,
                               slug            VARCHAR(70)  NOT NULL,
                               time_zone       VARCHAR(40)  NOT NULL DEFAULT 'America/Sao_Paulo',
                               active          BOOLEAN      NOT NULL DEFAULT TRUE,
                               created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               CONSTRAINT establishment_slug_uq UNIQUE (slug)
);
CREATE INDEX establishment_organization_id_idx ON establishment (organization_id);

CREATE TABLE app_user (
                          id            UUID         PRIMARY KEY,
                          name          VARCHAR(120) NOT NULL,
                          email         VARCHAR(254) NOT NULL,
                          password_hash VARCHAR(100) NOT NULL,
                          active        BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX app_user_email_uq ON app_user (lower(email));

CREATE TABLE membership (
                            id               UUID        PRIMARY KEY,
                            user_id          UUID        NOT NULL REFERENCES app_user (id),
                            establishment_id UUID        NOT NULL REFERENCES establishment (id),
                            role             VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')),
                            pin_hash         VARCHAR(100),
                            active           BOOLEAN     NOT NULL DEFAULT TRUE,
                            created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                            CONSTRAINT membership_user_establishment_uq UNIQUE (user_id, establishment_id)
);
CREATE INDEX membership_user_id_idx ON membership (user_id);
CREATE INDEX membership_establishment_id_idx ON membership (establishment_id);
