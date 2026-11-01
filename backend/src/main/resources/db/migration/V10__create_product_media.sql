-- Fotos dos produtos (Guia 10). O vídeo curto (Reels, Etapa 10) entra nesta mesma tabela,
-- com type = 'VIDEO'; por isso o nome é "media", e não "photo".
CREATE TABLE product_media (
    id               UUID         PRIMARY KEY,
    establishment_id UUID         NOT NULL REFERENCES establishment (id),
    product_id       UUID         NOT NULL REFERENCES product (id),
    type             VARCHAR(10)  NOT NULL CHECK (type IN ('IMAGE')),
    -- Pasta do arquivo no armazenamento (R2); cada tamanho é um arquivo dentro dela.
    storage_path     VARCHAR(200) NOT NULL,
    -- Medidas do tamanho grande: a tela reserva o espaço certo antes de a foto chegar.
    width            INT          NOT NULL CHECK (width > 0),
    height           INT          NOT NULL CHECK (height > 0),
    position         INT          NOT NULL CHECK (position >= 0),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX product_media_establishment_id_idx ON product_media (establishment_id);
CREATE INDEX product_media_product_id_idx ON product_media (product_id);
