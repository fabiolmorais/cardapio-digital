-- Avaliação do atendimento (RF40; etapa C5 do plano de continuação dos relatórios).
-- Uma por celular da mesa (sessão), de 1 a 5 estrelas, com os atalhos e um comentário curto, ligada à
-- comanda e ao garçom que mais atendeu. É dado do negócio: não se apaga.
CREATE TABLE service_rating (
    id                 UUID         PRIMARY KEY,
    establishment_id   UUID         NOT NULL REFERENCES establishment (id),
    session_id         UUID         NOT NULL REFERENCES table_session (id),
    table_id           UUID         NOT NULL REFERENCES dining_table (id),
    tab_id             UUID         REFERENCES tab (id),
    waiter_id          UUID         REFERENCES app_user (id),
    stars              SMALLINT     NOT NULL CHECK (stars BETWEEN 1 AND 5),
    tag_speed          BOOLEAN      NOT NULL DEFAULT FALSE,
    tag_friendliness   BOOLEAN      NOT NULL DEFAULT FALSE,
    tag_food           BOOLEAN      NOT NULL DEFAULT FALSE,
    tag_ambience       BOOLEAN      NOT NULL DEFAULT FALSE,
    comment            VARCHAR(280),
    created_at         TIMESTAMPTZ  NOT NULL,
    -- O toque em "Avaliar no Google" (REL-84), só com nota 4 ou 5.
    google_clicked_at  TIMESTAMPTZ,
    -- Nota baixa: quando e quem marcou o alerta como resolvido.
    resolved_at        TIMESTAMPTZ,
    resolved_by        UUID         REFERENCES app_user (id),
    CONSTRAINT service_rating_session_uq UNIQUE (session_id),
    CONSTRAINT service_rating_google CHECK (google_clicked_at IS NULL OR stars >= 4),
    CONSTRAINT service_rating_resolved CHECK ((resolved_at IS NULL) = (resolved_by IS NULL))
);
CREATE INDEX service_rating_establishment_idx ON service_rating (establishment_id, created_at);
CREATE INDEX service_rating_table_id_idx ON service_rating (table_id);
CREATE INDEX service_rating_tab_id_idx ON service_rating (tab_id);
CREATE INDEX service_rating_waiter_idx ON service_rating (waiter_id, created_at);
CREATE INDEX service_rating_resolved_by_idx ON service_rating (resolved_by);

-- O garçom vê a própria média no app? O restaurante decide; começa desligado.
CREATE TABLE rating_setting (
    establishment_id      UUID        PRIMARY KEY REFERENCES establishment (id),
    waiter_sees_average   BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at            TIMESTAMPTZ NOT NULL
);
