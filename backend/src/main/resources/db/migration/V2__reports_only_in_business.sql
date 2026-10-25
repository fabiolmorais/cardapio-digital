-- Relatórios passam a ser exclusivos dos planos Business e Enterprise (decisão de 25/10/2026).
-- O recurso ADVANCED_REPORTS vira REPORTS: não há mais relatórios "básicos" nos planos menores.
UPDATE plan
SET entitlements = entitlements - 'ADVANCED_REPORTS'
WHERE code IN ('essential', 'pro');

UPDATE plan
SET entitlements = (entitlements - 'ADVANCED_REPORTS') || '["REPORTS"]'::jsonb
WHERE code IN ('business', 'enterprise');
