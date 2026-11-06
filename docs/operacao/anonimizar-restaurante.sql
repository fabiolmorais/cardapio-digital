-- Anonimiza os dados pessoais de um restaurante (LGPD: o titular pede para apagar os dados).
--
-- Uso (na raiz do repositório, com o banco de destino), DEPOIS de exportar uma cópia para entregar:
--   psql "$DATABASE_URL" -v slug=sakura-sushi -f docs/operacao/anonimizar-restaurante.sql
--
-- Comandas, pedidos e chamados são permanentes (CLAUDE.md): eles ficam, sem ninguém identificável.
-- O que sai: o nome, o documento, o WhatsApp e o e-mail da empresa; a marca do restaurante; o nome,
-- o e-mail e a senha de quem só trabalha nele (quem também trabalha em outro restaurante continua
-- lá), e todos ficam desligados; as observações escritas nos pedidos; as sessões, os aparelhos e as inscrições de
-- notificação. O restaurante fica desativado (os QR Codes param) e a assinatura, cancelada.
-- Tudo numa transação: ou anonimiza tudo, ou nada.
BEGIN;

CREATE TEMP TABLE alvo ON COMMIT DROP AS
SELECT e.id AS establishment_id, e.organization_id
FROM establishment e
WHERE e.slug = :'slug';

CREATE TEMP TABLE pessoas ON COMMIT DROP AS
SELECT m.user_id
FROM membership m
JOIN alvo a ON a.establishment_id = m.establishment_id
WHERE NOT EXISTS (SELECT 1 FROM membership outra
                  WHERE outra.user_id = m.user_id AND outra.establishment_id <> a.establishment_id);

UPDATE app_user u SET name = 'Pessoa anonimizada', email = NULL, password_hash = NULL, active = FALSE
FROM pessoas p WHERE u.id = p.user_id;

-- O PIN fica (o banco exige PIN de quem é do salão), mas é só um hash, e a pessoa fica desligada.
UPDATE membership m SET active = FALSE
FROM alvo a WHERE m.establishment_id = a.establishment_id;

UPDATE organization o
SET name = 'Empresa anonimizada', document = '00000000000000', whatsapp = '00000000000',
    email = 'anonimizado-' || o.id || '@anonimizado.invalid'
FROM alvo a WHERE o.id = a.organization_id;

UPDATE establishment e
SET name = 'Restaurante anonimizado', active = FALSE, logo_path = NULL, instagram = NULL, google_review_url = NULL
FROM alvo a WHERE e.id = a.establishment_id;

UPDATE order_item i SET note = NULL
FROM alvo a WHERE i.establishment_id = a.establishment_id AND i.note IS NOT NULL;

UPDATE refresh_token t SET revoked_at = now()
FROM alvo a WHERE t.establishment_id = a.establishment_id AND t.revoked_at IS NULL;

UPDATE device d SET revoked_at = now()
FROM alvo a WHERE d.establishment_id = a.establishment_id AND d.revoked_at IS NULL;

DELETE FROM push_subscription s USING alvo a WHERE s.establishment_id = a.establishment_id;

DELETE FROM password_reset_token r USING pessoas p WHERE r.user_id = p.user_id;

UPDATE subscription s SET status = 'CANCELED'
FROM alvo a WHERE s.organization_id = a.organization_id;

SELECT (SELECT count(*) FROM alvo) AS restaurantes, (SELECT count(*) FROM pessoas) AS pessoas_anonimizadas;

COMMIT;
