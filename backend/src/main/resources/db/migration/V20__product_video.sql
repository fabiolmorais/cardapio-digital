-- O vídeo curto do prato (Guia 22) entra na mesma tabela das fotos, como a V10 já previa.
ALTER TABLE product_media DROP CONSTRAINT product_media_type_check;
ALTER TABLE product_media ADD CONSTRAINT product_media_type_check CHECK (type IN ('IMAGE', 'VIDEO'));

-- As medidas do vídeo só chegam quando o serviço de vídeo termina a conversão.
ALTER TABLE product_media ALTER COLUMN width DROP NOT NULL;
ALTER TABLE product_media ALTER COLUMN height DROP NOT NULL;

-- O id do vídeo no serviço de vídeo (Bunny Stream), a conversão e a duração.
ALTER TABLE product_media ADD COLUMN video_id VARCHAR(64);
ALTER TABLE product_media ADD COLUMN status VARCHAR(12) NOT NULL DEFAULT 'READY'
    CHECK (status IN ('PROCESSING', 'READY', 'FAILED'));
ALTER TABLE product_media ADD COLUMN duration_seconds INT CHECK (duration_seconds > 0);
-- Por que a conversão falhou (o dono vê): "o vídeo tem mais de 60 segundos", por exemplo.
ALTER TABLE product_media ADD COLUMN failure VARCHAR(140);

-- Foto: pronta, com medidas, sem vídeo. Vídeo: com o id; pronto só com medidas e duração.
ALTER TABLE product_media ADD CONSTRAINT product_media_kind_complete CHECK (
    (type = 'IMAGE' AND video_id IS NULL AND status = 'READY' AND width IS NOT NULL AND height IS NOT NULL)
    OR (type = 'VIDEO' AND video_id IS NOT NULL
        AND (status <> 'READY' OR (width IS NOT NULL AND height IS NOT NULL AND duration_seconds IS NOT NULL))));

-- O aviso do serviço de vídeo chega só com o id dele: é por ele que a mídia é achada.
CREATE UNIQUE INDEX product_media_video_id_uq ON product_media (video_id) WHERE video_id IS NOT NULL;
