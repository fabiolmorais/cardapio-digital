-- Eventos do celular no cardápio (REL-27, 28, 29; etapa C2): o prato aberto, o reel visto e o item
-- posto no carrinho. Anônimos: só a sessão da mesa (que já é anônima), sem IP nem aparelho. Ficam 13
-- meses (um a mais que o histórico do Business) e a tarefa de limpeza apaga o resto.
-- session_id sem chave estrangeira de propósito: a sessão da mesa pode ser apagada antes do evento.
CREATE TABLE menu_event (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    session_id       UUID        NOT NULL,
    product_id       UUID        NOT NULL REFERENCES product (id),
    kind             VARCHAR(12) NOT NULL CHECK (kind IN ('PRODUCT_VIEW', 'REEL_VIEW', 'CART_ADD')),
    quantity         SMALLINT    NOT NULL CHECK (quantity BETWEEN 1 AND 20),
    occurred_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX menu_event_establishment_idx ON menu_event (establishment_id, occurred_at);
CREATE INDEX menu_event_session_idx ON menu_event (session_id);
CREATE INDEX menu_event_product_id_idx ON menu_event (product_id);
CREATE INDEX menu_event_occurred_idx ON menu_event (occurred_at);
