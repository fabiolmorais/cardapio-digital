package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.MenuReportPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.CategorySales;
import br.com.cardapio.domain.report.model.VideoComparison;
import br.com.cardapio.domain.report.model.VariantSales;
import br.com.cardapio.domain.report.model.ProductRow;
import br.com.cardapio.domain.report.model.ProductPair;
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.ProductSales;
import br.com.cardapio.domain.report.model.ProductSort;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * As vendas por produto em SQL nativo, só leitura e parametrizado (ADR 0013). {@code sold}: os itens
 * das comandas fechadas do restaurante no intervalo (e no setor da mesa, se pedido), sem os cancelados
 * e sem as rodadas do rodízio, somados por produto. O produto e a categoria são os de hoje.
 */
@Component
class MenuReportPersistenceAdapter implements MenuReportPort {

    private static final String SOLD = """
            WITH sold AS (
                SELECT i.product_id, sum(i.quantity) AS quantity, sum(i.unit_price_cents * i.quantity) AS revenue
                FROM order_item i
                JOIN customer_order o ON o.id = i.order_id
                JOIN tab t ON t.id = o.tab_id
                JOIN dining_table dt ON dt.id = t.table_id
                WHERE i.establishment_id = :establishmentId
                  AND t.establishment_id = :establishmentId
                  AND t.status = 'CLOSED'
                  AND t.closed_at >= :start AND t.closed_at < :end
                  AND (CAST(:sectorId AS uuid) IS NULL OR dt.sector_id = CAST(:sectorId AS uuid))
                  AND i.status <> 'CANCELED'
                  AND i.tab_rodizio_id IS NULL
                GROUP BY i.product_id
            )
            """;

    /** Os itens das comandas fechadas no intervalo, do restaurante e, se pedido, do setor da mesa. */
    private static final String ITEMS = """
                FROM order_item i
                JOIN customer_order o ON o.id = i.order_id
                JOIN tab t ON t.id = o.tab_id
                JOIN dining_table dt ON dt.id = t.table_id
                WHERE i.establishment_id = :establishmentId
                  AND t.establishment_id = :establishmentId
                  AND t.status = 'CLOSED'
                  AND t.closed_at >= :start AND t.closed_at < :end
                  AND (CAST(:sectorId AS uuid) IS NULL OR dt.sector_id = CAST(:sectorId AS uuid))
            """;

    /** O mesmo, com o produto do restaurante (na categoria, se pedida). */
    private static final String ITEMS_WITH_PRODUCT = ITEMS.replace("WHERE i.establishment_id",
            "JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId\n"
                    + "            WHERE (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))\n"
                    + "              AND i.establishment_id");

    /** O produto do restaurante e a categoria dele (filtrada, se pedida). */
    private static final String PRODUCT = """
            JOIN product p ON p.id = s.product_id AND p.establishment_id = :establishmentId
            JOIN menu_category c ON c.id = p.category_id
            WHERE (CAST(:categoryId AS uuid) IS NULL OR c.id = CAST(:categoryId AS uuid))
            """;

    private static final RowMapper<ProductSales> PRODUCT_SALES = (rs, row) -> new ProductSales(
            new ProductId(rs.getObject("product_id", UUID.class)), rs.getString("name"), rs.getString("category"),
            rs.getInt("quantity"), Money.ofCents(rs.getLong("revenue")));

    private final JdbcClient jdbc;

    MenuReportPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private JdbcClient.StatementSpec sold(String sql, EstablishmentId establishmentId, Instant start, Instant end,
                                          Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return jdbc.sql(SOLD + sql)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .param("categoryId", category.map(MenuCategoryId::value).orElse(null), Types.OTHER);
    }

    @Override
    public List<CategorySales> byCategory(EstablishmentId establishmentId, Instant start, Instant end,
                                          Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return sold("""
                        SELECT c.id AS category_id, c.name AS name,
                               sum(s.quantity) AS quantity, sum(s.revenue) AS revenue
                        FROM sold s
                        """ + PRODUCT + """
                        GROUP BY c.id, c.name
                        ORDER BY revenue DESC, c.name
                        """,
                establishmentId, start, end, sector, category)
                .query((rs, row) -> new CategorySales(new MenuCategoryId(rs.getObject("category_id", UUID.class)),
                        rs.getString("name"), rs.getInt("quantity"), Money.ofCents(rs.getLong("revenue"))))
                .list();
    }

    @Override
    public ResultPage<ProductRow> products(EstablishmentId establishmentId, Instant start, Instant end,
                                           Instant comparedStart, Instant comparedEnd, Optional<SectorId> sector,
                                           Optional<MenuCategoryId> category, ProductSort sort, PageQuery page) {
        // A ordem é um parâmetro como os outros: os CASE dão null nas ordens que não são a pedida, e a
        // ordem cai no faturamento. "before" é o que os produtos à frente somam: a base da curva ABC.
        String ranked = """
                , previous AS (
                    SELECT i.product_id, sum(i.unit_price_cents * i.quantity) AS revenue
                """ + ITEMS.replace(":start", ":comparedStart").replace(":end", ":comparedEnd") + """
                      AND i.status <> 'CANCELED' AND i.tab_rodizio_id IS NULL
                    GROUP BY i.product_id
                ), canceled AS (
                    SELECT i.product_id, sum(i.quantity) AS units
                """ + ITEMS + """
                      AND i.status = 'CANCELED'
                    GROUP BY i.product_id
                ), ranked AS (
                    SELECT p.id AS product_id, p.name AS name, c.name AS category, s.quantity, s.revenue,
                           coalesce(sum(s.revenue) OVER (ORDER BY s.revenue DESC, s.quantity DESC, p.name, p.id
                                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS before,
                           sum(s.revenue) OVER () AS total
                    FROM sold s
                """ + PRODUCT + """
                )
                """;
        List<ProductRow> content = sold(ranked + """
                        SELECT r.*, coalesce(b.revenue, 0) AS compared, coalesce(x.units, 0) AS canceled
                        FROM ranked r
                        LEFT JOIN previous b ON b.product_id = r.product_id
                        LEFT JOIN canceled x ON x.product_id = r.product_id
                        ORDER BY CASE WHEN :sort = 'QUANTITY' THEN r.quantity END DESC NULLS LAST,
                                 CASE WHEN :sort = 'GROWTH' AND b.revenue > 0
                                      THEN (r.revenue - b.revenue)::numeric / b.revenue END DESC NULLS LAST,
                                 r.revenue DESC, r.quantity DESC, r.name, r.product_id
                        LIMIT :limit OFFSET :offset
                        """,
                establishmentId, start, end, sector, category)
                .param("comparedStart", OffsetDateTime.ofInstant(comparedStart, ZoneOffset.UTC))
                .param("comparedEnd", OffsetDateTime.ofInstant(comparedEnd, ZoneOffset.UTC))
                .param("sort", sort.name())
                .param("limit", page.size())
                .param("offset", (long) page.page() * page.size())
                .query((rs, row) -> new ProductRow(PRODUCT_SALES.mapRow(rs, row), Money.ofCents(rs.getLong("before")),
                        Money.ofCents(rs.getLong("total")), Money.ofCents(rs.getLong("compared")), rs.getInt("canceled")))
                .list();
        long total = sold("SELECT count(*) FROM sold s " + PRODUCT, establishmentId, start, end, sector, category)
                .query(Long.class)
                .single();
        return new ResultPage<>(content, page.page(), page.size(), total);
    }

    @Override
    public List<CanceledProductCount> canceled(EstablishmentId establishmentId, Instant start, Instant end,
                                               Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return items("""
                        SELECT p.name, nullif(btrim(i.cancel_reason), '') AS reason, sum(i.quantity) AS times
                        """ + ITEMS_WITH_PRODUCT + """
                          AND i.status = 'CANCELED'
                        GROUP BY p.name, nullif(btrim(i.cancel_reason), '')
                        """,
                establishmentId, start, end, sector, category)
                .query((rs, row) -> new CanceledProductCount(rs.getString("name"), rs.getString("reason"),
                        rs.getInt("times")))
                .list();
    }

    @Override
    public List<VariantSales> sizes(EstablishmentId establishmentId, Instant start, Instant end,
                                    Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return items("""
                        SELECT p.id AS product_id, p.name, i.variant_name, sum(i.quantity) AS quantity
                        """ + ITEMS_WITH_PRODUCT + """
                          AND i.status <> 'CANCELED' AND i.variant_name IS NOT NULL
                        GROUP BY p.id, p.name, i.variant_name
                        """,
                establishmentId, start, end, sector, category)
                .query((rs, row) -> new VariantSales(new ProductId(rs.getObject("product_id", UUID.class)),
                        rs.getString("name"), rs.getString("variant_name"), rs.getInt("quantity")))
                .list();
    }

    @Override
    public List<OptionSales> options(EstablishmentId establishmentId, Instant start, Instant end,
                                     Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        // charged_cents é o que a opção somou em uma unidade do item: vezes as unidades do item.
        return items("""
                        SELECT op.group_title, op.option_name, sum(op.quantity * i.quantity) AS times,
                               sum(op.charged_cents * i.quantity) AS charged, bool_or(op.price_cents > 0) AS priced
                        FROM order_item_option op
                        JOIN order_item i ON i.id = op.order_item_id
                        JOIN customer_order o ON o.id = i.order_id
                        JOIN tab t ON t.id = o.tab_id
                        JOIN dining_table dt ON dt.id = t.table_id
                        JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                        WHERE op.establishment_id = :establishmentId
                          AND i.establishment_id = :establishmentId
                          AND t.establishment_id = :establishmentId
                          AND t.status = 'CLOSED'
                          AND t.closed_at >= :start AND t.closed_at < :end
                          AND (CAST(:sectorId AS uuid) IS NULL OR dt.sector_id = CAST(:sectorId AS uuid))
                          AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
                          AND i.status <> 'CANCELED' AND NOT op.splits_item
                        GROUP BY op.group_title, op.option_name
                        """,
                establishmentId, start, end, sector, category)
                .query((rs, row) -> new OptionSales(rs.getString("group_title"), rs.getString("option_name"),
                        rs.getInt("times"), Money.ofCents(rs.getLong("charged")), rs.getBoolean("priced")))
                .list();
    }

    @Override
    public List<ProductPair> pairs(EstablishmentId establishmentId, Instant start, Instant end,
                                   Optional<SectorId> sector, int minTogether, int limit) {
        return items("""
                        WITH tp AS (
                            SELECT DISTINCT o.tab_id, i.product_id
                        """ + ITEMS + """
                              AND i.status <> 'CANCELED' AND i.tab_rodizio_id IS NULL
                        ), counts AS (
                            SELECT product_id, count(*) AS tabs FROM tp GROUP BY product_id
                        ), together AS (
                            SELECT a.product_id AS first, b.product_id AS second, count(*) AS together
                            FROM tp a JOIN tp b ON b.tab_id = a.tab_id AND b.product_id > a.product_id
                            GROUP BY a.product_id, b.product_id
                            HAVING count(*) >= :minTogether
                            ORDER BY together DESC
                            LIMIT :limit
                        )
                        SELECT pa.name AS first_name, pb.name AS second_name, ca.tabs AS first_tabs,
                               cb.tabs AS second_tabs, x.together
                        FROM together x
                        JOIN counts ca ON ca.product_id = x.first
                        JOIN counts cb ON cb.product_id = x.second
                        JOIN product pa ON pa.id = x.first AND pa.establishment_id = :establishmentId
                        JOIN product pb ON pb.id = x.second AND pb.establishment_id = :establishmentId
                        """,
                establishmentId, start, end, sector, Optional.empty())
                .param("minTogether", minTogether)
                .param("limit", limit)
                .query((rs, row) -> new ProductPair(rs.getString("first_name"), rs.getString("second_name"),
                        rs.getInt("first_tabs"), rs.getInt("second_tabs"), rs.getInt("together")))
                .list();
    }

    @Override
    public VideoComparison video(EstablishmentId establishmentId, Instant start, Instant end,
                                 Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return sold("""
                        , products AS (
                            SELECT p.id, coalesce(s.quantity, 0) AS sold,
                                   EXISTS (SELECT 1 FROM product_media m
                                           WHERE m.product_id = p.id AND m.establishment_id = :establishmentId
                                             AND m.type = 'VIDEO' AND m.status = 'READY' AND m.active) AS video
                            FROM product p
                            LEFT JOIN sold s ON s.product_id = p.id
                            WHERE p.establishment_id = :establishmentId AND p.active AND p.a_la_carte
                              AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
                        )
                        SELECT count(*) FILTER (WHERE video) AS with_video,
                               coalesce(sum(sold) FILTER (WHERE video), 0) AS with_video_sold,
                               count(*) FILTER (WHERE NOT video) AS without_video,
                               coalesce(sum(sold) FILTER (WHERE NOT video), 0) AS without_video_sold
                        FROM products
                        """,
                establishmentId, start, end, sector, category)
                .query((rs, row) -> new VideoComparison(rs.getInt("with_video"), rs.getInt("with_video_sold"),
                        rs.getInt("without_video"), rs.getInt("without_video_sold")))
                .single();
    }

    private JdbcClient.StatementSpec items(String sql, EstablishmentId establishmentId, Instant start, Instant end,
                                           Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return jdbc.sql(sql)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .param("categoryId", category.map(MenuCategoryId::value).orElse(null), Types.OTHER);
    }

    @Override
    public List<ProductSales> slowSellers(EstablishmentId establishmentId, Instant start, Instant end,
                                          Optional<SectorId> sector, Optional<MenuCategoryId> category,
                                          Instant createdUntil, int limit) {
        return sold("""
                        SELECT p.id AS product_id, p.name AS name, c.name AS category,
                               coalesce(s.quantity, 0) AS quantity, coalesce(s.revenue, 0) AS revenue
                        FROM product p
                        JOIN menu_category c ON c.id = p.category_id
                        LEFT JOIN sold s ON s.product_id = p.id
                        WHERE p.establishment_id = :establishmentId
                          AND p.active AND p.a_la_carte
                          AND p.created_at <= :createdUntil
                          AND (CAST(:categoryId AS uuid) IS NULL OR c.id = CAST(:categoryId AS uuid))
                        ORDER BY quantity, revenue, p.name, p.id
                        LIMIT :limit
                        """,
                establishmentId, start, end, sector, category)
                .param("createdUntil", OffsetDateTime.ofInstant(createdUntil, ZoneOffset.UTC))
                .param("limit", limit)
                .query(PRODUCT_SALES)
                .list();
    }

    @Override
    public int activeProducts(EstablishmentId establishmentId, Optional<MenuCategoryId> category) {
        return jdbc.sql("""
                        SELECT count(*) FROM product p
                        WHERE p.establishment_id = :establishmentId AND p.active AND p.a_la_carte
                          AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = CAST(:categoryId AS uuid))
                        """)
                .param("establishmentId", establishmentId.value())
                .param("categoryId", category.map(MenuCategoryId::value).orElse(null), Types.OTHER)
                .query(Integer.class)
                .single();
    }
}
