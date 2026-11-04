-- O rodízio de passagem na mesa e o cartão verde/vermelho (Guia 27).

-- O tipo é copiado na ativação, como o nome: mudar o cadastro depois não muda a mesa que já começou.
ALTER TABLE tab_rodizio ADD COLUMN rodizio_type VARCHAR(12) NOT NULL DEFAULT 'A_LA_MINUTE'
    CHECK (rodizio_type IN ('A_LA_MINUTE', 'PASSAGEM'));

-- O cartão: SERVING é o verde (pode servir), PAUSED é o vermelho. A hora da última virada mostra
-- aos passadores "pausada há 3 min".
ALTER TABLE tab_rodizio ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'SERVING'
    CHECK (status IN ('SERVING', 'PAUSED'));
ALTER TABLE tab_rodizio ADD COLUMN status_changed_at TIMESTAMPTZ;
UPDATE tab_rodizio SET status_changed_at = started_at;
ALTER TABLE tab_rodizio ALTER COLUMN status_changed_at SET NOT NULL;
