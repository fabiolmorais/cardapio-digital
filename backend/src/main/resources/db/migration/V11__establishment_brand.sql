-- Marca e aparência (Guia 11): o que o cliente vê na página da mesa e no display impresso.
-- Ficam no próprio estabelecimento: é uma marca por restaurante, e o nome já mora aqui.
ALTER TABLE establishment
    -- Uma das cores da paleta (BrandColor), e não um código de cor livre: cada cor da paleta já
    -- tem os tons de texto que mantêm a leitura no tema escuro e no claro.
    ADD COLUMN brand_color       VARCHAR(20)  NOT NULL DEFAULT 'TOMATO'
        CHECK (brand_color IN ('TOMATO', 'WINE', 'MUSTARD', 'OLIVE', 'PETROL', 'INDIGO')),
    ADD COLUMN theme             VARCHAR(10)  NOT NULL DEFAULT 'DARK' CHECK (theme IN ('DARK', 'LIGHT')),
    -- Pasta da logo no armazenamento; vazia = a página mostra as iniciais.
    ADD COLUMN logo_path         VARCHAR(200),
    ADD COLUMN instagram         VARCHAR(30),
    ADD COLUMN google_review_url VARCHAR(300);
