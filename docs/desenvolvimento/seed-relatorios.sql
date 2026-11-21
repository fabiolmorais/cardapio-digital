-- Dados FICTÍCIOS de 12 meses para desenvolver e medir os relatórios (ADR 0013). Só local ou staging.
--
-- Uso (na raiz do repositório, com o banco local do Docker Compose):
--   docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
--     -v slug=<slug-do-restaurante> -v confirmar=sim -f -' < docs/desenvolvimento/seed-relatorios.sql
--
-- Antes: crie o restaurante pelo cadastro do app (o slug aparece na URL da página pública).
--
-- O que faz, num restaurante SEM nenhuma comanda (com comandas, para e não muda nada):
-- - se faltarem, cria os setores Salão (12 mesas) e Deck (6 mesas), um cardápio genérico e três
--   garçons (sem PIN válido: servem só de nome nos relatórios), cadastrados antes do ano de movimento;
-- - cria, para cada dia dos últimos 365 (até ontem), as visitas do dia: comanda aberta e fechada,
--   celular da mesa, pedidos (pelo celular ou lançados pelo garçom), itens com os horários de
--   preparo, pronto e entregue (alguns cancelados, com motivo), chamados de garçom e de conta,
--   pagamento e a taxa de serviço de 10% (aceita na maioria das mesas);
-- - o movimento segue o dia da semana, o almoço e o jantar, e cresce ao longo do ano.
-- Determinístico (setseed): rodar em dois restaurantes novos gera os mesmos números. Os tokens das
-- mesas e dos celulares vêm de gen_random_uuid() (fora do setseed), para não repetirem.
-- Não cria rodízio nem dados novos da fase 2.

\set ON_ERROR_STOP on

SELECT set_config('seed.slug', :'slug', false), set_config('seed.confirmar', :'confirmar', false);

DO $seed$
DECLARE
    v_est        uuid;
    v_tz         text;
    v_owner      uuid;
    v_staff      uuid[];
    v_tables     uuid[];
    v_products   uuid[];
    v_names      text[];
    v_prices     bigint[];
    v_drink      boolean[];
    v_sector     uuid;
    v_category   uuid;
    v_day        date;
    v_dow        int;
    v_growth     numeric;
    v_count      int;
    v_tab        uuid;
    v_table      uuid;
    v_session    uuid;
    v_order      uuid;
    v_opened     timestamptz;
    v_closed     timestamptz;
    v_hour       int;
    v_total      bigint;
    v_service    bigint;
    v_accepted   boolean;
    v_method     text;
    v_waiter     uuid;
    v_orders     int;
    v_items      int;
    v_pos        int;
    v_idx        int;
    v_qty        int;
    v_created    timestamptz;
    v_prep       int;
    v_ready      timestamptz;
    v_last       timestamptz;
    v_delivered  timestamptz;
    v_number     int;
    v_peak       boolean;
    v_canceled   boolean;
    v_by_phone   boolean;
    v_reason     text;
    v_prepaid    bigint;
    v_since      timestamptz := now() - interval '366 days';
    i            int;
    o            int;
    k            int;
BEGIN
    IF current_setting('seed.confirmar') <> 'sim' THEN
        RAISE EXCEPTION 'Passe -v confirmar=sim: este script cria dados fictícios no restaurante';
    END IF;

    SELECT id, time_zone INTO v_est, v_tz FROM establishment WHERE slug = current_setting('seed.slug');
    IF v_est IS NULL THEN
        RAISE EXCEPTION 'Restaurante não encontrado: %', current_setting('seed.slug');
    END IF;
    IF EXISTS (SELECT 1 FROM tab WHERE establishment_id = v_est) THEN
        RAISE EXCEPTION 'O restaurante já tem comandas: o seed só roda num restaurante sem movimento';
    END IF;

    PERFORM setseed(0.42);

    SELECT m.user_id INTO v_owner FROM membership m
    WHERE m.establishment_id = v_est AND m.role = 'OWNER' AND m.active LIMIT 1;

    -- Salão e mesas
    IF NOT EXISTS (SELECT 1 FROM dining_table WHERE establishment_id = v_est AND active) THEN
        FOR k IN 1..2 LOOP
            v_sector := gen_random_uuid();
            INSERT INTO sector (id, establishment_id, name, position, created_at)
            VALUES (v_sector, v_est, CASE k WHEN 1 THEN 'Salão' ELSE 'Deck' END, k - 1, v_since);
            FOR i IN 1..(CASE k WHEN 1 THEN 12 ELSE 6 END) LOOP
                INSERT INTO dining_table (id, establishment_id, sector_id, label, public_token, token_issued_at,
                                          created_at)
                VALUES (gen_random_uuid(), v_est, v_sector,
                        CASE k WHEN 1 THEN lpad(i::text, 2, '0') ELSE 'D' || i END,
                        replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''), now(), v_since);
            END LOOP;
        END LOOP;
    END IF;
    SELECT array_agg(id ORDER BY label) INTO v_tables FROM dining_table WHERE establishment_id = v_est AND active;

    -- Cardápio genérico (nome, preço em centavos, é bebida?), do mais pedido para o menos pedido
    IF NOT EXISTS (SELECT 1 FROM product WHERE establishment_id = v_est AND active AND a_la_carte) THEN
        FOR k IN 1..5 LOOP
            v_category := gen_random_uuid();
            INSERT INTO menu_category (id, establishment_id, name, position, created_at)
            VALUES (v_category, v_est, (ARRAY['Pratos', 'Petiscos', 'Lanches', 'Bebidas', 'Sobremesas'])[k], k - 1,
                    v_since);
            INSERT INTO product (id, establishment_id, category_id, name, price_cents, position, created_at)
            SELECT gen_random_uuid(), v_est, v_category, p.name, p.price, p.ord, v_since
            FROM (VALUES
                      (1, 'Prato da casa', 6890, 0), (1, 'Costela no bafo', 7990, 1), (1, 'Risoto de camarão', 6490, 2),
                      (1, 'Salada de quinoa', 3990, 3),
                      (2, 'Porção de fritas', 3290, 0), (2, 'Bolinho de bacalhau', 3890, 1), (2, 'Isca de peixe', 4490, 2),
                      (3, 'Hambúrguer artesanal', 3990, 0), (3, 'Pizza grande', 7490, 1), (3, 'Sanduíche de pernil', 3490, 2),
                      (4, 'Chopp 500 ml', 1590, 0), (4, 'Refrigerante lata', 690, 1), (4, 'Caipirinha', 2290, 2),
                      (4, 'Água mineral', 490, 3), (4, 'Suco detox', 1490, 4),
                      (5, 'Sobremesa da casa', 2490, 0), (5, 'Petit gâteau', 2890, 1)
                 ) AS p(cat, name, price, ord)
            WHERE p.cat = k;
        END LOOP;
    END IF;
    -- A ordem de popularidade: a posição dentro da categoria, intercalando as categorias
    SELECT array_agg(p.id ORDER BY p.position, c.position), array_agg(p.name ORDER BY p.position, c.position),
           array_agg(p.price_cents ORDER BY p.position, c.position),
           array_agg(c.name ILIKE '%bebida%' ORDER BY p.position, c.position)
    INTO v_products, v_names, v_prices, v_drink
    FROM product p JOIN menu_category c ON c.id = p.category_id
    WHERE p.establishment_id = v_est AND p.active AND p.a_la_carte;

    -- Garçons
    IF NOT EXISTS (SELECT 1 FROM membership WHERE establishment_id = v_est AND role = 'STAFF' AND active) THEN
        FOR k IN 1..3 LOOP
            v_waiter := gen_random_uuid();
            INSERT INTO app_user (id, name, created_at) VALUES (v_waiter, (ARRAY['Carlos', 'Ana', 'Rodrigo'])[k], v_since);
            INSERT INTO membership (id, user_id, establishment_id, role, pin_hash, created_at)
            VALUES (gen_random_uuid(), v_waiter, v_est, 'STAFF', 'seed-sem-pin', v_since);
        END LOOP;
    END IF;
    SELECT array_agg(user_id ORDER BY created_at, user_id) INTO v_staff
    FROM membership WHERE establishment_id = v_est AND role = 'STAFF' AND active;

    -- Um ano de movimento, até ontem
    FOR v_day IN SELECT generate_series(current_date - 365, current_date - 1, interval '1 day')::date LOOP
        v_dow := extract(dow FROM v_day);
        v_growth := 0.85 + 0.25 * (365 - (current_date - v_day)) / 365.0;
        v_count := round((ARRAY[70, 30, 38, 45, 52, 85, 95])[v_dow + 1] * v_growth * (0.85 + random() * 0.3));
        v_number := 0;

        FOR i IN 1..v_count LOOP
            -- Almoço (11h às 14h) ou jantar (18h às 23h); no domingo, o almoço pesa mais
            IF random() < (CASE WHEN v_dow = 0 THEN 0.55 ELSE 0.3 END) THEN
                v_hour := 11 + floor(random() * random() * 4)::int;
            ELSE
                v_hour := 18 + least(5, floor(abs(random() + random() - 1) * 4 + 2)::int + (random() < 0.3)::int - 1);
            END IF;
            v_peak := v_hour BETWEEN 20 AND 22 AND v_dow IN (5, 6);
            v_opened := ((v_day + make_time(v_hour, floor(random() * 60)::int, 0)) AT TIME ZONE v_tz);
            v_table := v_tables[1 + floor(random() * array_length(v_tables, 1))::int];
            v_waiter := v_staff[1 + floor(power(random(), 1.4) * array_length(v_staff, 1))::int];
            v_tab := gen_random_uuid();
            v_session := gen_random_uuid();

            -- O celular da mesa (em 85% das visitas alguém escaneou o QR)
            IF random() < 0.85 THEN
                INSERT INTO table_session (id, establishment_id, table_id, token_hash, created_at, last_seen_at,
                                           expires_at, ended_at, presence_method, presence_confirmed_at, unlocked_by)
                SELECT v_session, v_est, v_table, replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''), v_opened, v_opened,
                       v_opened + interval '4 hours', v_opened + interval '3 hours', m.method, v_opened,
                       CASE WHEN m.method = 'STAFF' THEN v_waiter END
                FROM (SELECT CASE WHEN r < 0.81 THEN 'WIFI' WHEN r < 0.93 THEN 'GPS' ELSE 'STAFF' END AS method
                      FROM (SELECT random() AS r) x) m;
            ELSE
                v_session := NULL;
            END IF;

            INSERT INTO tab (id, establishment_id, table_id, status, opened_at)
            VALUES (v_tab, v_est, v_table, 'OPEN', v_opened);

            v_total := 0;
            v_created := v_opened;
            v_last := v_opened;
            v_orders := 1 + floor(random() * random() * 4)::int;
            FOR o IN 1..v_orders LOOP
                v_created := v_created + make_interval(mins => 2 + floor(random() * 25)::int);
                v_order := gen_random_uuid();
                v_number := v_number + 1;
                -- Pelo celular (75% das mesas com celular) ou lançado pelo garçom: uma origem só
                v_by_phone := v_session IS NOT NULL AND random() < 0.75;
                INSERT INTO customer_order (id, establishment_id, tab_id, table_id, session_id, number, status,
                                            created_at, placed_by)
                VALUES (v_order, v_est, v_tab, v_table, CASE WHEN v_by_phone THEN v_session END,
                        v_number, 'SENT', v_created, CASE WHEN v_by_phone THEN NULL ELSE v_waiter END);

                v_items := 1 + floor(random() * 3)::int;
                FOR v_pos IN 1..v_items LOOP
                    v_idx := 1 + floor(power(random(), 1.8) * array_length(v_products, 1))::int;
                    v_qty := CASE WHEN v_drink[v_idx] THEN 1 + floor(random() * 3)::int ELSE 1 + (random() < 0.2)::int END;
                    v_prep := CASE WHEN v_drink[v_idx] THEN 2 + floor(random() * 3)::int
                                   ELSE 8 + floor(random() * 10)::int + v_idx % 5 END;
                    IF v_peak THEN
                        v_prep := round(v_prep * 1.4);
                    END IF;
                    v_ready := v_created + make_interval(mins => 1 + v_prep);
                    v_canceled := random() < 0.025;
                    v_delivered := v_ready + make_interval(secs => 30 + floor(random() * (CASE WHEN v_peak THEN 480 ELSE 200 END))::int);
                    IF NOT v_canceled THEN
                        v_last := greatest(v_last, v_delivered);
                    END IF;
                    v_reason := (ARRAY['Acabou o ingrediente', 'Cliente desistiu', 'Pedido repetido'])
                        [1 + floor(random() * 3)::int];
                    INSERT INTO order_item (id, establishment_id, order_id, position, product_id, product_name,
                                            unit_price_cents, quantity, status, created_at, preparing_at, ready_at,
                                            delivered_at, delivered_by, canceled_at, canceled_by, cancel_reason)
                    VALUES (gen_random_uuid(), v_est, v_order, v_pos - 1, v_products[v_idx], v_names[v_idx],
                            v_prices[v_idx], v_qty,
                            CASE WHEN v_canceled THEN 'CANCELED' ELSE 'DELIVERED' END, v_created,
                            CASE WHEN v_canceled THEN NULL ELSE v_created + interval '1 minute' END,
                            CASE WHEN v_canceled THEN NULL ELSE v_ready END,
                            CASE WHEN v_canceled THEN NULL ELSE v_delivered END,
                            CASE WHEN v_canceled THEN NULL ELSE v_waiter END,
                            CASE WHEN v_canceled THEN v_created + interval '3 minutes' END,
                            CASE WHEN v_canceled THEN v_waiter END,
                            CASE WHEN v_canceled THEN v_reason END);
                    IF NOT v_canceled THEN
                        v_total := v_total + v_prices[v_idx] * v_qty;
                    END IF;
                END LOOP;
            END LOOP;

            -- Fecha depois da última entrega (e de uma permanência de 40 a 140 minutos)
            v_closed := greatest(v_last, v_opened + make_interval(mins => 40 + floor(random() * 100)::int))
                        + make_interval(mins => 10);

            -- Chamados do garçom (alguns desistidos) durante a visita, e o de conta no fim
            IF v_session IS NOT NULL THEN
                FOR k IN 1..floor(random() * 3)::int LOOP
                    v_created := v_opened + (v_closed - v_opened - interval '20 minutes') * random();
                    IF random() < 0.03 THEN
                        INSERT INTO call (id, establishment_id, table_id, session_id, type, status, created_at, canceled_at)
                        VALUES (gen_random_uuid(), v_est, v_table, v_session, 'WAITER', 'CANCELED', v_created,
                                v_created + make_interval(mins => 4 + floor(random() * 5)::int));
                    ELSE
                        INSERT INTO call (id, establishment_id, table_id, session_id, type, status, created_at,
                                          accepted_at, accepted_by, done_at)
                        VALUES (gen_random_uuid(), v_est, v_table, v_session, 'WAITER', 'DONE', v_created,
                                v_created + make_interval(secs => 40 + floor(random() * (CASE WHEN v_peak THEN 200 ELSE 90 END))::int),
                                v_waiter, v_created + make_interval(secs => 200 + floor(random() * 180)::int));
                    END IF;
                END LOOP;
            END IF;

            v_method := CASE WHEN random() < 0.46 THEN 'PIX' WHEN random() < 0.81 THEN 'CARD' ELSE 'CASH' END;
            IF v_session IS NOT NULL THEN
                INSERT INTO call (id, establishment_id, table_id, session_id, type, payment_method, status, created_at,
                                  accepted_at, accepted_by, done_at)
                VALUES (gen_random_uuid(), v_est, v_table, v_session, 'BILL', v_method, 'DONE',
                        v_closed - interval '7 minutes', v_closed - interval '6 minutes', v_waiter, v_closed);
            END IF;

            v_accepted := random() < 0.85;
            v_service := CASE WHEN v_accepted THEN round(v_total * 0.10) ELSE 0 END;
            -- 4% das mesas pagam uma parte adiantada (quem sai antes)
            v_prepaid := CASE WHEN random() < 0.04 AND v_total > 4000 THEN (v_total + v_service) / 2 ELSE 0 END;
            IF v_prepaid > 0 THEN
                INSERT INTO tab_payment (id, establishment_id, tab_id, amount_cents, payment_method, received_at, received_by)
                VALUES (gen_random_uuid(), v_est, v_tab, v_prepaid, 'PIX', v_closed - interval '30 minutes', v_waiter);
            END IF;
            IF v_total + v_service - v_prepaid > 0 THEN
                INSERT INTO tab_payment (id, establishment_id, tab_id, amount_cents, payment_method, received_at, received_by)
                VALUES (gen_random_uuid(), v_est, v_tab, v_total + v_service - v_prepaid, v_method, v_closed, v_waiter);
            END IF;
            UPDATE tab SET status = 'CLOSED', closed_at = v_closed, closed_by = v_waiter, total_cents = v_total,
                           payment_method = v_method, service_percent = 10, service_accepted = v_accepted,
                           service_cents = v_service, service_destination = 'TEAM'
            WHERE id = v_tab;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Seed pronto: % comandas, % itens',
        (SELECT count(*) FROM tab WHERE establishment_id = v_est),
        (SELECT count(*) FROM order_item WHERE establishment_id = v_est);
END
$seed$;
