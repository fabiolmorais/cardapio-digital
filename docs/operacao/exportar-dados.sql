-- Exporta os dados de um restaurante num JSON só (LGPD: o titular pede uma cópia dos dados).
--
-- Uso (na raiz do repositório, com o banco de destino):
--   psql "$DATABASE_URL" -v slug=sakura-sushi -At -f docs/operacao/exportar-dados.sql > sakura-sushi.json
--
-- Vai tudo o que é do restaurante: a empresa, o restaurante, a equipe (sem senha, PIN nem tokens),
-- o salão, o cardápio, os rodízios, as comandas com os pedidos e a assinatura. Nenhuma linha se o
-- slug não existir.
SELECT json_build_object(
    'exportado_em', now(),
    'empresa', json_build_object('nome', o.name, 'documento', o.document, 'whatsapp', o.whatsapp,
                                 'email', o.email, 'criada_em', o.created_at),
    'restaurante', json_build_object('nome', e.name, 'slug', e.slug, 'fuso', e.time_zone, 'ativo', e.active,
                                     'cor', e.brand_color, 'tema', e.theme, 'instagram', e.instagram,
                                     'avaliacao_google', e.google_review_url, 'criado_em', e.created_at),
    'equipe', COALESCE((SELECT json_agg(json_build_object('nome', u.name, 'email', u.email, 'papel', m.role,
                                                          'ativo', m.active, 'desde', m.created_at)
                                        ORDER BY m.created_at)
                        FROM membership m JOIN app_user u ON u.id = m.user_id
                        WHERE m.establishment_id = e.id), '[]'),
    'setores', COALESCE((SELECT json_agg(json_build_object('nome', s.name, 'ativo', s.active) ORDER BY s.position)
                         FROM sector s WHERE s.establishment_id = e.id), '[]'),
    'mesas', COALESCE((SELECT json_agg(json_build_object('mesa', t.label, 'setor', s.name, 'ativa', t.active)
                                       ORDER BY t.label)
                       FROM dining_table t JOIN sector s ON s.id = t.sector_id
                       WHERE t.establishment_id = e.id), '[]'),
    'cardapio', COALESCE((SELECT json_agg(json_build_object(
                              'categoria', c.name, 'ativa', c.active,
                              'produtos', COALESCE((SELECT json_agg(json_build_object(
                                                        'nome', p.name, 'descricao', p.description,
                                                        'preco_centavos', p.price_cents,
                                                        'promocao_centavos', p.promo_price_cents, 'ativo', p.active)
                                                    ORDER BY p.position)
                                                    FROM product p WHERE p.category_id = c.id), '[]'))
                          ORDER BY c.position)
                          FROM menu_category c WHERE c.establishment_id = e.id), '[]'),
    'rodizios', COALESCE((SELECT json_agg(json_build_object('nome', r.name, 'tipo', r.type, 'ativo', r.active)
                                          ORDER BY r.created_at)
                          FROM rodizio r WHERE r.establishment_id = e.id), '[]'),
    'comandas', COALESCE((SELECT json_agg(json_build_object(
                              'mesa', t.label, 'status', tb.status, 'aberta_em', tb.opened_at,
                              'fechada_em', tb.closed_at, 'total_centavos', tb.total_cents,
                              'pagamento', tb.payment_method,
                              'pedidos', COALESCE((SELECT json_agg(json_build_object(
                                                       'numero', co.number, 'status', co.status,
                                                       'feito_em', co.created_at,
                                                       'itens', COALESCE((SELECT json_agg(json_build_object(
                                                                              'produto', oi.product_name,
                                                                              'quantidade', oi.quantity,
                                                                              'preco_centavos', oi.unit_price_cents,
                                                                              'observacao', oi.note,
                                                                              'status', oi.status)
                                                                          ORDER BY oi.position)
                                                                          FROM order_item oi
                                                                          WHERE oi.order_id = co.id), '[]'))
                                                   ORDER BY co.number)
                                                   FROM customer_order co WHERE co.tab_id = tb.id), '[]'))
                          ORDER BY tb.opened_at)
                          FROM tab tb JOIN dining_table t ON t.id = tb.table_id
                          WHERE tb.establishment_id = e.id), '[]'),
    'assinatura', (SELECT json_build_object('plano', p.name, 'status', s.status, 'teste_ate', s.trial_ends_at,
                                            'pago_ate', s.current_period_end)
                   FROM subscription s JOIN plan p ON p.id = s.plan_id
                   WHERE s.organization_id = o.id)
) AS dados
FROM establishment e
JOIN organization o ON o.id = e.organization_id
WHERE e.slug = :'slug';
