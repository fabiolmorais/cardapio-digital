-- Ativa (ou renova) a assinatura de um restaurante que pagou.
--
-- Uso (na raiz do repositório, com o banco de destino):
--   psql "$DATABASE_URL" -v slug=sakura-sushi -f docs/operacao/ativar-assinatura.sql
--
-- O que faz:
-- - passa para o plano e o ciclo que o dono escolheu na tela "Assinatura" (sem escolha, mantém
--   o plano atual, no ciclo mensal);
-- - marca como ACTIVE, pago até 1 mês (ou 1 ano) depois do fim do que já estava coberto: quem
--   paga antes do fim do teste não perde os dias que faltavam;
-- - limpa a escolha, que já foi atendida.
-- Rodar de novo no mês seguinte renova por mais um período.
UPDATE subscription s
SET plan_id            = COALESCE(s.requested_plan_id, s.plan_id),
    billing_cycle      = COALESCE(s.requested_cycle, s.billing_cycle, 'MONTHLY'),
    status             = 'ACTIVE',
    current_period_end = GREATEST(now(), CASE WHEN s.status = 'TRIALING' THEN s.trial_ends_at
                                              ELSE s.current_period_end END)
                         + CASE COALESCE(s.requested_cycle, s.billing_cycle, 'MONTHLY')
                               WHEN 'ANNUAL' THEN INTERVAL '1 year'
                               ELSE INTERVAL '1 month' END,
    requested_plan_id  = NULL,
    requested_cycle    = NULL,
    requested_at       = NULL
FROM establishment e
WHERE e.organization_id = s.organization_id
  AND e.slug = :'slug'
RETURNING e.name AS restaurante,
          (SELECT p.name FROM plan p WHERE p.id = s.plan_id) AS plano,
          s.billing_cycle AS ciclo,
          s.current_period_end AS pago_ate;
