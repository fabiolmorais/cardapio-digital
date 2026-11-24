package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.MenuInterestReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.report.model.CartSessions;
import br.com.cardapio.domain.report.model.ProductInterestRow;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Os eventos do celular em SQL nativo e parametrizado, sempre por restaurante. Parte dos eventos do
 * período (índice por restaurante e hora) e procura os pedidos pela sessão (índice da sessão no pedido).
 */
@Component
class MenuInterestReportPersistenceAdapter implements MenuInterestReportPort {

    /** Com setor, só as sessões de mesas do setor. */
    private static String inSector(Optional<SectorId> sector) {
        return sector.isEmpty() ? " TRUE " : " " + """
                 e.session_id IN (SELECT s.id FROM table_session s
                                  JOIN dining_table dt ON dt.id = s.table_id AND dt.establishment_id = :establishmentId
                                  WHERE s.establishment_id = :establishmentId
                                    AND dt.sector_id = CAST(:sectorId AS uuid))
                """;
    }

    /**
     * Por sessão e produto: o que os eventos do período dizem (viu, viu o reel, pôs no carrinho) e se a
     * mesma sessão pediu o produto (um pedido não recusado, a qualquer hora). Eventos e pedidos vão numa
     * lista só, agrupada por hash: uma junção entre os dois fazia o Postgres errar a estimativa e
     * comparar par a par (2,5 s com 30 dias no seed). Só ficam os pares com algum evento, de produtos do
     * restaurante (na categoria, se pedida).
     */
    private static String pairs(Optional<SectorId> sector) {
        String period = " " + """
                e.establishment_id = :establishmentId AND e.occurred_at >= :start AND e.occurred_at < :end
                  AND""" + inSector(sector);
        return """
                SELECT x.session_id, x.product_id,
                       bool_or(x.kind IN ('PRODUCT_VIEW', 'REEL_VIEW')) AS viewed,
                       bool_or(x.kind = 'REEL_VIEW') AS reel,
                       coalesce(sum(x.quantity) FILTER (WHERE x.kind = 'CART_ADD'), 0) AS added,
                       bool_or(x.kind = 'ORDER') AS ordered
                FROM (
                    SELECT e.session_id, e.product_id, e.kind, e.quantity
                    FROM menu_event e
                    JOIN product p ON p.id = e.product_id AND p.establishment_id = :establishmentId
                    WHERE (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
                      AND""" + period + """
                    UNION ALL
                    SELECT o.session_id, i.product_id, 'ORDER', 0
                    FROM customer_order o
                    JOIN order_item i ON i.order_id = o.id AND i.establishment_id = :establishmentId
                    WHERE o.establishment_id = :establishmentId AND o.status <> 'REJECTED'
                      AND o.session_id IN (SELECT e.session_id FROM menu_event e WHERE""" + period + """
                    )
                ) x
                GROUP BY x.session_id, x.product_id
                HAVING bool_or(x.kind <> 'ORDER')
                """;
    }

    private final JdbcClient jdbc;

    MenuInterestReportPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private JdbcClient.StatementSpec sql(String sql, EstablishmentId establishmentId, Instant start, Instant end,
                                         Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return jdbc.sql(sql)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .param("categoryId", category.map(MenuCategoryId::value).orElse(null), Types.OTHER);
    }

    @Override
    public Optional<Instant> recordingSince(EstablishmentId establishmentId) {
        return jdbc.sql("SELECT min(occurred_at) AS since FROM menu_event WHERE establishment_id = :establishmentId")
                .param("establishmentId", establishmentId.value())
                .query((rs, row) -> Optional.ofNullable(rs.getObject("since", OffsetDateTime.class))
                        .map(OffsetDateTime::toInstant))
                .single();
    }

    /** Os pares calculados uma vez (materializados); os carrinhos saem deles, em colunas calculadas uma vez só. */
    @Override
    public Found find(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
                      Optional<MenuCategoryId> category) {
        List<ProductInterestRow> rows = new ArrayList<>();
        int[] carts = {0, 0};
        sql("WITH m AS MATERIALIZED (" + pairs(sector) + """
                        ), carts AS (
                            SELECT DISTINCT session_id FROM m WHERE added > 0
                        )
                        SELECT p.name, p.price_cents,
                               count(*) FILTER (WHERE m.viewed) AS viewers,
                               count(*) FILTER (WHERE m.viewed AND m.ordered) AS viewers_ordered,
                               count(*) FILTER (WHERE m.reel) AS reel_viewers,
                               count(*) FILTER (WHERE m.reel AND m.ordered) AS reel_ordered,
                               count(*) FILTER (WHERE m.added > 0) AS cart_sessions,
                               count(*) FILTER (WHERE m.added > 0 AND NOT m.ordered) AS left_sessions,
                               coalesce(sum(m.added) FILTER (WHERE NOT m.ordered), 0) AS left_units,
                               (SELECT count(*) FROM carts) AS with_cart,
                               (SELECT count(*) FROM carts c
                                WHERE NOT EXISTS (SELECT 1 FROM customer_order o
                                                  WHERE o.establishment_id = :establishmentId
                                                    AND o.session_id = c.session_id)) AS not_sent
                        FROM m
                        JOIN product p ON p.id = m.product_id AND p.establishment_id = :establishmentId
                        GROUP BY p.id, p.name, p.price_cents
                        """, establishmentId, start, end, sector, category)
                .query(rs -> {
                    carts[0] = rs.getInt("with_cart");
                    carts[1] = rs.getInt("not_sent");
                    rows.add(new ProductInterestRow(rs.getString("name"),
                            Money.ofCents(rs.getLong("price_cents")), rs.getInt("viewers"),
                            rs.getInt("viewers_ordered"), rs.getInt("reel_viewers"), rs.getInt("reel_ordered"),
                            rs.getInt("cart_sessions"), rs.getInt("left_sessions"), rs.getInt("left_units")));
                });
        return new Found(rows, new CartSessions(carts[0], carts[1]));
    }
}
