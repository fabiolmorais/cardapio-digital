package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.SalesReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.DailyRevenue;
import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * As somas das vendas em SQL nativo, só leitura e parametrizado (ADR 0013). Toda consulta parte das
 * comandas fechadas do restaurante ({@code tab_closed_idx}: establishment_id, closed_at); comanda
 * juntada ({@code MERGED}) não conta, porque o que ela tinha já está na de destino.
 */
@Component
class SalesReportPersistenceAdapter implements SalesReportPort {

    /** As comandas fechadas no intervalo, no restaurante e, se pedido, no setor da mesa. */
    private static final String CLOSED_TABS = """
            FROM tab t
            JOIN dining_table dt ON dt.id = t.table_id
            WHERE t.establishment_id = :establishmentId
              AND t.status = 'CLOSED'
              AND t.closed_at >= :start AND t.closed_at < :end
              AND (CAST(:sectorId AS uuid) IS NULL OR dt.sector_id = CAST(:sectorId AS uuid))
            """;

    private final JdbcClient jdbc;

    SalesReportPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private JdbcClient.StatementSpec closedTabs(String sql, EstablishmentId establishmentId, Instant start,
                                                Instant end, Optional<SectorId> sector) {
        return jdbc.sql(sql)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), java.sql.Types.OTHER);
    }

    @Override
    public ReportTotals totals(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector) {
        return closedTabs("SELECT coalesce(sum(t.total_cents), 0) AS revenue, count(*) AS tabs " + CLOSED_TABS,
                establishmentId, start, end, sector)
                .query((rs, row) -> new ReportTotals(Money.ofCents(rs.getLong("revenue")), rs.getInt("tabs")))
                .single();
    }

    @Override
    public List<DailyRevenue> revenueByDay(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                           Optional<SectorId> sector) {
        return closedTabs("""
                        SELECT (t.closed_at AT TIME ZONE :zone)::date AS day,
                               sum(t.total_cents) AS revenue, count(*) AS tabs
                        """ + CLOSED_TABS + " GROUP BY 1 ORDER BY 1",
                establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .query((rs, row) -> new DailyRevenue(rs.getObject("day", LocalDate.class),
                        Money.ofCents(rs.getLong("revenue")), rs.getInt("tabs")))
                .list();
    }

    @Override
    public List<PaymentTotal> payments(EstablishmentId establishmentId, Instant start, Instant end,
                                       Optional<SectorId> sector) {
        return closedTabs("""
                        SELECT p.payment_method AS method, sum(p.amount_cents) AS amount
                        FROM tab_payment p
                        JOIN (SELECT t.id\s""" + CLOSED_TABS + """
                        ) closed ON closed.id = p.tab_id
                        WHERE p.establishment_id = :establishmentId
                        GROUP BY 1 ORDER BY 2 DESC
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new PaymentTotal(PaymentMethod.valueOf(rs.getString("method")),
                        Money.ofCents(rs.getLong("amount"))))
                .list();
    }

    @Override
    public List<HourlyRevenue> revenueByWeekdayAndHour(EstablishmentId establishmentId, Instant start, Instant end,
                                                       ZoneId zone, Optional<SectorId> sector) {
        return closedTabs("""
                        SELECT extract(isodow FROM t.opened_at AT TIME ZONE :zone)::int AS weekday,
                               extract(hour FROM t.opened_at AT TIME ZONE :zone)::int AS hour,
                               sum(t.total_cents) AS revenue, count(*) AS tabs
                        """ + CLOSED_TABS + " GROUP BY 1, 2",
                establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .query((rs, row) -> new HourlyRevenue(rs.getInt("weekday"), rs.getInt("hour"),
                        Money.ofCents(rs.getLong("revenue")), rs.getInt("tabs")))
                .list();
    }

    @Override
    public List<ItemSales> topItems(EstablishmentId establishmentId, Instant start, Instant end,
                                    Optional<SectorId> sector, int limit) {
        // O nome é o do cadastro hoje (o que o dono reconhece), mesmo que o produto tenha mudado de nome.
        return closedTabs("""
                        SELECT i.product_id AS product_id, p.name AS name,
                               sum(i.quantity) AS quantity, sum(i.unit_price_cents * i.quantity) AS revenue
                        FROM order_item i
                        JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                        JOIN customer_order o ON o.id = i.order_id
                        JOIN (SELECT t.id\s""" + CLOSED_TABS + """
                        ) closed ON closed.id = o.tab_id
                        WHERE i.establishment_id = :establishmentId
                          AND i.status <> 'CANCELED'
                          AND i.tab_rodizio_id IS NULL
                        GROUP BY i.product_id, p.name
                        ORDER BY revenue DESC, quantity DESC, name
                        LIMIT :limit
                        """,
                establishmentId, start, end, sector)
                .param("limit", limit)
                .query((rs, row) -> new ItemSales(new ProductId(rs.getObject("product_id", UUID.class)),
                        rs.getString("name"), rs.getInt("quantity"), Money.ofCents(rs.getLong("revenue"))))
                .list();
    }
}
