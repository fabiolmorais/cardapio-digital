-- Quantas pessoas o prato serve (opcional): null quando o dono prefere não informar.
-- O teto (20) fica no domínio; aqui só o que nunca faz sentido.
ALTER TABLE product
    ADD COLUMN serves INTEGER CHECK (serves > 0);
