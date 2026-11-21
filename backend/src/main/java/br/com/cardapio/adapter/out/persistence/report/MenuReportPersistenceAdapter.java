package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.MenuReportPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.CategorySales;
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
    public ResultPage<ProductSales> products(EstablishmentId establishmentId, Instant start, Instant end,
                                             Optional<SectorId> sector, Optional<MenuCategoryId> category,
                                             ProductSort sort, PageQuery page) {
        // A ordem é um parâmetro como os outros: por quantidade, o CASE ordena primeiro; por faturamento,
        // ele dá null em todas as linhas e a ordem cai no faturamento.
        List<ProductSales> content = sold("""
                        SELECT p.id AS product_id, p.name AS name, c.name AS category, s.quantity, s.revenue
                        FROM sold s
                        """ + PRODUCT + """
                        ORDER BY CASE WHEN :sort = 'QUANTITY' THEN s.quantity END DESC NULLS LAST,
                                 s.revenue DESC, s.quantity DESC, p.name, p.id
                        LIMIT :limit OFFSET :offset
                        """,
                establishmentId, start, end, sector, category)
                .param("sort", sort.name())
                .param("limit", page.size())
                .param("offset", (long) page.page() * page.size())
                .query(PRODUCT_SALES)
                .list();
        long total = sold("SELECT count(*) FROM sold s " + PRODUCT, establishmentId, start, end, sector, category)
                .query(Long.class)
                .single();
        return new ResultPage<>(content, page.page(), page.size(), total);
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
