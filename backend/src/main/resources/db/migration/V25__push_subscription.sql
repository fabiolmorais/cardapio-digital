-- As notificações no celular da equipe (Guia 29): uma inscrição por navegador (o endpoint do
-- serviço de push é único). Quem recebe é decidido no envio, pela equipe ativa do restaurante.
CREATE TABLE push_subscription (
    id               UUID          PRIMARY KEY,
    establishment_id UUID          NOT NULL REFERENCES establishment (id),
    user_id          UUID          NOT NULL REFERENCES app_user (id),
    endpoint         VARCHAR(1000) NOT NULL UNIQUE,
    p256dh           VARCHAR(100)  NOT NULL,
    auth             VARCHAR(40)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL
);
CREATE INDEX push_subscription_establishment_id_idx ON push_subscription (establishment_id);
CREATE INDEX push_subscription_user_id_idx ON push_subscription (user_id);
