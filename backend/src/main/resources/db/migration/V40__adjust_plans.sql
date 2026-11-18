-- Novos preços e recursos dos planos (decisão de 18/11/2026). O anual continua valendo 10 mensalidades.
-- O Essencial passa a ter vídeos, filas de cozinha e bar e perfis e permissões.
UPDATE plan
SET monthly_price_cents = 11990,
    annual_price_cents  = 119900,
    entitlements        = '["PRODUCT_VIDEOS","PREP_STATIONS","ROLES_AND_PERMISSIONS"]'::jsonb
WHERE code = 'essential';

UPDATE plan
SET monthly_price_cents = 14990,
    annual_price_cents  = 149900
WHERE code = 'pro';
