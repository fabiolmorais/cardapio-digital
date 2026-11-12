-- O rodízio é da mesa, mas quem participa é cada celular: o celular que chega depois da ativação
-- pergunta se vai participar e só pede rodadas depois que o garçom o inclui (ajustando as pessoas).
CREATE TABLE rodizio_phone (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    tab_rodizio_id   UUID        NOT NULL REFERENCES tab_rodizio (id),
    session_id       UUID        NOT NULL REFERENCES table_session (id),
    -- Quando o celular disse "vou participar" (e chamou o garçom); NULL se entrou na ativação.
    requested_at     TIMESTAMPTZ,
    -- Quando entrou no rodízio (na ativação, ou quando o garçom ajustou as pessoas); NULL enquanto espera.
    joined_at        TIMESTAMPTZ,
    CONSTRAINT rodizio_phone_uq UNIQUE (tab_rodizio_id, session_id),
    CONSTRAINT rodizio_phone_has_moment CHECK (requested_at IS NOT NULL OR joined_at IS NOT NULL)
);
CREATE INDEX rodizio_phone_establishment_id_idx ON rodizio_phone (establishment_id);
CREATE INDEX rodizio_phone_session_id_idx ON rodizio_phone (session_id);

-- Os celulares que já estão nas mesas com rodízio ativo continuam nele, como até agora.
INSERT INTO rodizio_phone (id, establishment_id, tab_rodizio_id, session_id, joined_at)
SELECT gen_random_uuid(), s.establishment_id, r.id, s.id, now()
  FROM tab_rodizio r
  JOIN tab t ON t.id = r.tab_id AND t.status IN ('OPEN', 'CLOSING')
  JOIN table_session s ON s.table_id = t.table_id AND s.ended_at IS NULL AND s.expires_at > now();
