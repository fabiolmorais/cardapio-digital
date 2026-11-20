-- Relatórios básicos a partir do Pro (decisão de 20/11/2026). Os básicos são vendas, cardápio, picos e
-- tempos de preparo; os completos (equipe, atendimento, ocupação, metas, alertas) continuam no Business
-- e no Enterprise, que também recebem os básicos para a verificação ser a mesma em todos os planos.
UPDATE plan
SET entitlements = entitlements || '["BASIC_REPORTS"]'::jsonb
WHERE code IN ('pro', 'business', 'enterprise')
  AND NOT entitlements ? 'BASIC_REPORTS';
