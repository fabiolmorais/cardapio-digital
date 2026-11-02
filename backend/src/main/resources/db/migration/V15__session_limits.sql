-- Fim da trava de presença (Guia 17): a validade da sessão, o bloqueio pela equipe e a limpeza.

-- Quanto tempo sem uso a sessão da mesa aguenta, escolhido pelo restaurante (2 horas, se ele não mudar).
ALTER TABLE presence_rule
    ADD COLUMN session_idle_minutes INTEGER NOT NULL DEFAULT 120,
    ADD CONSTRAINT presence_rule_session_idle_range CHECK (session_idle_minutes BETWEEN 30 AND 360);

-- Cada sessão guarda o prazo com que nasceu (mudar a configuração não mexe nas abertas),
-- e o bloqueio pela equipe: quando e quem.
ALTER TABLE table_session
    ADD COLUMN idle_minutes INTEGER     NOT NULL DEFAULT 120,
    ADD COLUMN blocked_at   TIMESTAMPTZ,
    ADD COLUMN blocked_by   UUID REFERENCES app_user (id),
    ADD CONSTRAINT table_session_idle_range CHECK (idle_minutes BETWEEN 30 AND 360),
    ADD CONSTRAINT table_session_block_complete CHECK ((blocked_at IS NULL) = (blocked_by IS NULL));
CREATE INDEX table_session_blocked_by_idx ON table_session (blocked_by);

-- A tarefa agendada procura as sessões abertas que já venceram: o índice parcial só tem as abertas.
CREATE INDEX table_session_open_expires_idx ON table_session (expires_at) WHERE ended_at IS NULL;
