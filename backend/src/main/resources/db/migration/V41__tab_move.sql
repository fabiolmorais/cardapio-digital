-- Mudar a comanda de mesa e juntar comandas (RF44, ADR 0012).

-- A comanda juntada a outra: fica registrada, sem total, apontando para a que continuou.
ALTER TABLE tab DROP CONSTRAINT tab_status_check;
ALTER TABLE tab ADD CONSTRAINT tab_status_check CHECK (status IN ('OPEN', 'CLOSING', 'CLOSED', 'MERGED'));
ALTER TABLE tab
    ADD COLUMN merged_into_tab_id UUID REFERENCES tab (id),
    ADD CONSTRAINT tab_merged_complete CHECK ((status = 'MERGED') = (merged_into_tab_id IS NOT NULL));
CREATE INDEX tab_merged_into_tab_id_idx ON tab (merged_into_tab_id);

-- Cada mudança de mesa: de onde, para onde, quem e quando. Na junção, merged_tab_id é a comanda que
-- deixou de existir (a de origem); tab_id é a que continuou.
-- needs_print: havia itens a caminho já impressos (tab_move_item), e a estação de impressão solta a
-- via "MUDOU DE MESA"; printed_at diz quando ela saiu.
CREATE TABLE tab_move (
    id               UUID        PRIMARY KEY,
    establishment_id UUID        NOT NULL REFERENCES establishment (id),
    tab_id           UUID        NOT NULL REFERENCES tab (id),
    from_table_id    UUID        NOT NULL REFERENCES dining_table (id),
    to_table_id      UUID        NOT NULL REFERENCES dining_table (id),
    merged_tab_id    UUID REFERENCES tab (id),
    moved_by         UUID        NOT NULL REFERENCES app_user (id),
    moved_at         TIMESTAMPTZ NOT NULL,
    needs_print      BOOLEAN     NOT NULL DEFAULT FALSE,
    printed_at       TIMESTAMPTZ,
    CONSTRAINT tab_move_other_table CHECK (from_table_id <> to_table_id),
    CONSTRAINT tab_move_printed_only_if_needed CHECK (needs_print OR printed_at IS NULL)
);
CREATE INDEX tab_move_establishment_id_idx ON tab_move (establishment_id);
CREATE INDEX tab_move_tab_id_idx ON tab_move (tab_id, moved_at);
CREATE INDEX tab_move_from_table_id_idx ON tab_move (from_table_id, moved_at);
CREATE INDEX tab_move_to_table_id_idx ON tab_move (to_table_id);
CREATE INDEX tab_move_merged_tab_id_idx ON tab_move (merged_tab_id);
CREATE INDEX tab_move_moved_by_idx ON tab_move (moved_by);
-- A estação de impressão procura as vias que faltam.
CREATE INDEX tab_move_print_pending_idx ON tab_move (establishment_id, moved_at) WHERE needs_print AND printed_at IS NULL;

-- Os itens a caminho que já tinham saído na impressora com a mesa antiga: são eles que vão na via.
CREATE TABLE tab_move_item (
    tab_move_id      UUID NOT NULL REFERENCES tab_move (id),
    order_item_id    UUID NOT NULL REFERENCES order_item (id),
    establishment_id UUID NOT NULL REFERENCES establishment (id),
    PRIMARY KEY (tab_move_id, order_item_id)
);
CREATE INDEX tab_move_item_order_item_id_idx ON tab_move_item (order_item_id);
CREATE INDEX tab_move_item_establishment_id_idx ON tab_move_item (establishment_id);
