-- Pedidos pelo celular (Guia 18): a comanda, o pedido, os itens e as configurações.

-- Como o restaurante recebe pedidos. Uma linha por restaurante; sem linha, valem os padrões.
CREATE TABLE order_settings (
    id                UUID        PRIMARY KEY,
    establishment_id  UUID        NOT NULL REFERENCES establishment (id),
    ordering_enabled  BOOLEAN     NOT NULL,
    approval_required BOOLEAN     NOT NULL,
    -- Limites: acima deles, o pedido espera o garçom. NULL = sem limite.
    max_amount_cents  BIGINT      CHECK (max_amount_cents >= 100),
    max_items         INTEGER     CHECK (max_items BETWEEN 1 AND 100),
    updated_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT order_settings_establishment_uq UNIQUE (establishment_id)
);

-- A comanda da mesa: abre no primeiro pedido e junta os pedidos de todos os celulares.
CREATE TABLE tab (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    table_id         UUID        NOT NULL REFERENCES dining_table (id),
    status           VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'CLOSING', 'CLOSED')),
    opened_at        TIMESTAMPTZ NOT NULL,
    closed_at        TIMESTAMPTZ,
    closed_by        UUID REFERENCES app_user (id),
    CONSTRAINT tab_closed_complete CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL))
);
CREATE INDEX tab_establishment_id_idx ON tab (establishment_id);
CREATE INDEX tab_table_id_idx ON tab (table_id);
CREATE INDEX tab_closed_by_idx ON tab (closed_by);
-- No máximo UMA comanda aberta por mesa, mesmo com dois celulares mandando o primeiro pedido juntos.
CREATE UNIQUE INDEX tab_one_open_per_table_uq ON tab (table_id) WHERE status IN ('OPEN', 'CLOSING');

-- O pedido (um envio do carrinho). "order" é palavra reservada do SQL: a tabela é customer_order.
CREATE TABLE customer_order (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    tab_id           UUID        NOT NULL REFERENCES tab (id),
    table_id         UUID        NOT NULL REFERENCES dining_table (id),
    session_id       UUID        NOT NULL REFERENCES table_session (id),
    number           INTEGER     NOT NULL CHECK (number >= 1),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'SENT', 'REJECTED')),
    created_at       TIMESTAMPTZ NOT NULL,
    approved_by      UUID REFERENCES app_user (id),
    approved_at      TIMESTAMPTZ
);
CREATE INDEX customer_order_establishment_id_idx ON customer_order (establishment_id);
CREATE INDEX customer_order_tab_id_idx ON customer_order (tab_id);
CREATE INDEX customer_order_table_id_idx ON customer_order (table_id);
CREATE INDEX customer_order_session_id_idx ON customer_order (session_id);
CREATE INDEX customer_order_approved_by_idx ON customer_order (approved_by);

-- Os itens. O nome e o preço são COPIADOS do cardápio no momento do pedido.
CREATE TABLE order_item (
    id               UUID         PRIMARY KEY,
    establishment_id UUID         NOT NULL REFERENCES establishment (id),
    order_id         UUID         NOT NULL REFERENCES customer_order (id),
    -- A posição no carrinho: os itens de um pedido têm o mesmo horário, e é por ela que voltam na ordem.
    position         INTEGER      NOT NULL,
    product_id       UUID         NOT NULL REFERENCES product (id),
    product_name     VARCHAR(80)  NOT NULL,
    unit_price_cents BIGINT       NOT NULL CHECK (unit_price_cents >= 0),
    quantity         INTEGER      NOT NULL CHECK (quantity BETWEEN 1 AND 20),
    note             VARCHAR(140),
    status           VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'PREPARING', 'READY', 'DELIVERED', 'CANCELED')),
    created_at       TIMESTAMPTZ  NOT NULL,
    preparing_at     TIMESTAMPTZ,
    ready_at         TIMESTAMPTZ,
    delivered_at     TIMESTAMPTZ,
    delivered_by     UUID REFERENCES app_user (id),
    canceled_at      TIMESTAMPTZ,
    canceled_by      UUID REFERENCES app_user (id),
    cancel_reason    VARCHAR(140)
);
CREATE INDEX order_item_establishment_id_idx ON order_item (establishment_id);
CREATE INDEX order_item_order_id_idx ON order_item (order_id);
CREATE INDEX order_item_product_id_idx ON order_item (product_id);
CREATE INDEX order_item_delivered_by_idx ON order_item (delivered_by);
CREATE INDEX order_item_canceled_by_idx ON order_item (canceled_by);
-- A fila da cozinha (Guia 19): os itens a caminho do restaurante, por ordem de chegada.
CREATE INDEX order_item_queue_idx ON order_item (establishment_id, created_at)
    WHERE status IN ('PENDING', 'PREPARING', 'READY');

-- O número do pedido no dia, por restaurante: a linha do dia guarda o último número dado.
CREATE TABLE order_counter (
    establishment_id UUID    NOT NULL REFERENCES establishment (id),
    day              DATE    NOT NULL,
    last_number      INTEGER NOT NULL,
    PRIMARY KEY (establishment_id, day)
);
