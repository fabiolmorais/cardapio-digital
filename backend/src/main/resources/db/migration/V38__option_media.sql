-- Opções com foto, descrição e vídeo, e o preço da opção por produto (complemento da ADR 0010).

-- A descrição da opção (os ingredientes do sabor), como a do produto.
ALTER TABLE option_item ADD COLUMN description VARCHAR(300);

-- A mídia passa a ter um dono: um produto OU uma opção (exatamente um dos dois).
ALTER TABLE product_media ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE product_media ADD COLUMN option_item_id UUID REFERENCES option_item (id);
ALTER TABLE product_media ADD CONSTRAINT product_media_one_owner CHECK (num_nonnulls(product_id, option_item_id) = 1);
CREATE INDEX product_media_option_item_id_idx ON product_media (option_item_id);

-- O preço da opção neste produto, sem tamanho (cada tamanho da pizza é um produto). Um preço por
-- produto, opção e tamanho (vazio = o produto todo).
ALTER TABLE option_item_price ALTER COLUMN product_variant_id DROP NOT NULL;
ALTER TABLE option_item_price DROP CONSTRAINT option_item_price_option_item_id_product_variant_id_key;
ALTER TABLE option_item_price ADD CONSTRAINT option_item_price_unique
    UNIQUE NULLS NOT DISTINCT (product_id, option_item_id, product_variant_id);
CREATE INDEX option_item_price_option_item_id_idx ON option_item_price (option_item_id);
