-- Resumo diário dos relatórios (REL-76, R4): o e-mail e o aviso no celular do fim do expediente.

-- A configuração do restaurante. Sem linha, vale o padrão: e-mail e celular ligados, às 2h.
-- O "dia" do resumo vai do horário escolhido de um dia até o mesmo horário do dia seguinte: quem
-- fecha às 2h recebe a sexta inteira. last_sent_day: o último dia já enviado (a marcação vem antes
-- do envio, para nenhum resumo sair duas vezes, mesmo com várias instâncias).
CREATE TABLE daily_summary_setting (
    establishment_id UUID        PRIMARY KEY REFERENCES establishment (id),
    email_enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    push_enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    send_hour        INTEGER     NOT NULL DEFAULT 2 CHECK (send_hour BETWEEN 0 AND 23),
    last_sent_day    DATE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Quem recebe: o dono e os gerentes, cada um pode sair. Começa ligado.
ALTER TABLE membership
    ADD COLUMN daily_summary BOOLEAN NOT NULL DEFAULT TRUE;
