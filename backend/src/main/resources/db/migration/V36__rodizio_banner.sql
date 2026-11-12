-- O banner do rodízio (tela "Escolha o rodízio"): o caminho da imagem no armazenamento. NULL: a
-- mesa mostra a imagem automática (a foto de um prato).
ALTER TABLE rodizio ADD COLUMN banner_path VARCHAR(200);
