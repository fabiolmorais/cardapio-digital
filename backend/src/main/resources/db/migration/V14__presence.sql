-- Trava de presença (Guia 16): só quem está no salão chama o garçom e pede.

-- As regras de cada restaurante: as redes do Wi-Fi do salão e o local com o raio. Uma por restaurante.
CREATE TABLE presence_rule (
    id               UUID             PRIMARY KEY,
    establishment_id UUID             NOT NULL REFERENCES establishment (id),
    -- Faixas de IP da internet do salão, no formato CIDR ("189.45.212.18/32", "2804:14c:6a::/64").
    networks         VARCHAR(50)[]    NOT NULL DEFAULT '{}',
    -- O nome da rede, para a tela da mesa dizer "conecte-se ao Wi-Fi Sakura Clientes". Nunca a senha.
    wifi_name        VARCHAR(60),
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    radius_meters    INTEGER,
    updated_at       TIMESTAMPTZ      NOT NULL,
    CONSTRAINT presence_rule_establishment_uq UNIQUE (establishment_id),
    -- O local vem inteiro (latitude, longitude e raio) ou não vem.
    CONSTRAINT presence_rule_location_complete CHECK (
        (latitude IS NULL) = (longitude IS NULL) AND (latitude IS NULL) = (radius_meters IS NULL)),
    CONSTRAINT presence_rule_latitude_range CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT presence_rule_longitude_range CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT presence_rule_radius_range CHECK (radius_meters BETWEEN 25 AND 1000)
);

-- Como a presença da sessão foi provada, quando, e (na liberação) quem liberou.
ALTER TABLE table_session
    ADD COLUMN presence_method       VARCHAR(10) CHECK (presence_method IN ('WIFI', 'GPS', 'STAFF')),
    ADD COLUMN presence_confirmed_at TIMESTAMPTZ,
    ADD COLUMN unlocked_by           UUID REFERENCES app_user (id),
    ADD CONSTRAINT table_session_presence_complete CHECK ((presence_method IS NULL) = (presence_confirmed_at IS NULL)),
    ADD CONSTRAINT table_session_unlock_by_staff CHECK ((presence_method = 'STAFF') = (unlocked_by IS NOT NULL));
CREATE INDEX table_session_unlocked_by_idx ON table_session (unlocked_by);

-- O chamado novo: "Liberar esta mesa". O CHECK do tipo (V13) não tinha nome; o Postgres o chamou de call_type_check.
ALTER TABLE call DROP CONSTRAINT call_type_check;
ALTER TABLE call ADD CONSTRAINT call_type_check CHECK (type IN ('WAITER', 'BILL', 'UNLOCK'));
