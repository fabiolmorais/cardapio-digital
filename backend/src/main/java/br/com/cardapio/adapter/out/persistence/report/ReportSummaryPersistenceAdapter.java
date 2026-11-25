package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.ReportSummaryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PrepTotal;
import br.com.cardapio.domain.report.model.SummaryStatus;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O resumo diário por produto (ADR 0014), em SQL nativo parametrizado. Refazer apaga os dias e grava
 * de novo, a partir das mesmas regras das consultas diretas:
 * <ul>
 *   <li>vendidos: itens não cancelados e fora do rodízio das comandas fechadas, no dia em que a comanda
 *   fechou e no setor da mesa da comanda (como os itens mais vendidos);</li>
 *   <li>preparos: itens não cancelados que ficaram prontos, no dia em que foram pedidos e no setor da
 *   mesa do pedido, com o tempo da chegada à cozinha até pronto (como o preparo médio).</li>
 * </ul>
 */
@Component
class ReportSummaryPersistenceAdapter implements ReportSummaryPort {

    private final JdbcClient jdbc;

    ReportSummaryPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LocalDate> summarizedUntil(EstablishmentId establishmentId) {
        return jdbc.sql("SELECT summarized_until FROM report_summary_state WHERE establishment_id = :establishmentId")
                .param("establishmentId", establishmentId.value())
                .query((rs, row) -> rs.getDate("summarized_until").toLocalDate())
                .optional();
    }

    @Override
    public List<ItemSales> sales(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                 Optional<SectorId> sector) {
        return jdbc.sql("""
                        SELECT r.product_id, p.name, sum(r.sold_quantity) AS quantity,
                               sum(r.sold_revenue_cents) AS revenue
                        FROM report_product_day r
                        JOIN product p ON p.id = r.product_id AND p.establishment_id = :establishmentId
                        WHERE r.establishment_id = :establishmentId
                          AND r.day >= :from AND r.day < :until
                          AND (CAST(:sectorId AS uuid) IS NULL OR r.sector_id = CAST(:sectorId AS uuid))
                        GROUP BY r.product_id, p.name
                        HAVING sum(r.sold_quantity) > 0
                        """)
                .param("establishmentId", establishmentId.value())
                .param("from", Date.valueOf(from))
                .param("until", Date.valueOf(until))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .query((rs, row) -> new ItemSales(new ProductId(rs.getObject("product_id", UUID.class)),
                        rs.getString("name"), rs.getInt("quantity"), Money.ofCents(rs.getLong("revenue"))))
                .list();
    }

    @Override
    public List<PrepTotal> preparations(EstablishmentId establishmentId, LocalDate from, LocalDate until,
                                        Optional<SectorId> sector) {
        return jdbc.sql("""
                        SELECT r.product_id, p.name, sum(r.preparations) AS preparations,
                               sum(r.prep_millis) AS millis
                        FROM report_product_day r
                        JOIN product p ON p.id = r.product_id AND p.establishment_id = :establishmentId
                        WHERE r.establishment_id = :establishmentId
                          AND r.day >= :from AND r.day < :until
                          AND (CAST(:sectorId AS uuid) IS NULL OR r.sector_id = CAST(:sectorId AS uuid))
                        GROUP BY r.product_id, p.name
                        HAVING sum(r.preparations) > 0
                        """)
                .param("establishmentId", establishmentId.value())
                .param("from", Date.valueOf(from))
                .param("until", Date.valueOf(until))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .query((rs, row) -> new PrepTotal(new ProductId(rs.getObject("product_id", UUID.class)),
                        rs.getString("name"), rs.getInt("preparations"), rs.getLong("millis")))
                .list();
    }

    /** O primeiro dia com movimento só é procurado para quem ainda não tem resumo (é a única hora em que importa). */
    @Override
    public List<SummaryStatus> statuses() {
        return jdbc.sql("""
                        SELECT e.id, e.time_zone, s.summarized_until,
                               CASE WHEN s.establishment_id IS NULL THEN least(
                                   (SELECT min(t.closed_at) FROM tab t
                                    WHERE t.establishment_id = e.id AND t.status = 'CLOSED'),
                                   (SELECT min(i.created_at) FROM order_item i WHERE i.establishment_id = e.id))
                               END AS first_activity
                        FROM establishment e
                        LEFT JOIN report_summary_state s ON s.establishment_id = e.id
                        WHERE e.active
                        """)
                .query((rs, row) -> {
                    ZoneId zone = ZoneId.of(rs.getString("time_zone"));
                    Date until = rs.getDate("summarized_until");
                    OffsetDateTime first = rs.getObject("first_activity", OffsetDateTime.class);
                    return new SummaryStatus(new EstablishmentId(rs.getObject("id", UUID.class)), zone,
                            Optional.ofNullable(until).map(Date::toLocalDate),
                            Optional.ofNullable(first).map(at -> LocalDate.ofInstant(at.toInstant(), zone)));
                })
                .list();
    }

    @Override
    public void rebuild(EstablishmentId establishmentId, ZoneId zone, LocalDate from, LocalDate until, Instant now) {
        if (from.isBefore(until)) {
            jdbc.sql("DELETE FROM report_product_day WHERE establishment_id = :establishmentId "
                            + "AND day >= :from AND day < :until")
                    .param("establishmentId", establishmentId.value())
                    .param("from", Date.valueOf(from))
                    .param("until", Date.valueOf(until))
                    .update();
            jdbc.sql("""
                            INSERT INTO report_product_day (establishment_id, day, sector_id, product_id, sold_quantity,
                                                            sold_revenue_cents, preparations, prep_millis)
                            SELECT :establishmentId, day, sector_id, product_id, sum(sold), sum(revenue),
                                   sum(preparations), sum(millis)
                            FROM (
                                SELECT CAST(t.closed_at AT TIME ZONE :zone AS date) AS day, dt.sector_id, i.product_id,
                                       i.quantity AS sold, i.unit_price_cents * i.quantity AS revenue,
                                       0 AS preparations, 0 AS millis
                                FROM order_item i
                                JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                                JOIN customer_order o ON o.id = i.order_id
                                JOIN tab t ON t.id = o.tab_id
                                JOIN dining_table dt ON dt.id = t.table_id
                                WHERE i.establishment_id = :establishmentId
                                  AND t.establishment_id = :establishmentId
                                  AND t.status = 'CLOSED'
                                  AND t.closed_at >= :start AND t.closed_at < :end
                                  AND i.status <> 'CANCELED'
                                  AND i.tab_rodizio_id IS NULL
                                UNION ALL
                                SELECT CAST(i.created_at AT TIME ZONE :zone AS date), dt.sector_id, i.product_id,
                                       0, 0, 1,
                                       round(1000 * greatest(0, extract(epoch FROM i.ready_at
                                           - greatest(i.created_at, coalesce(o.approved_at, i.created_at)))))
                                FROM order_item i
                                JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                                JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                                JOIN dining_table dt ON dt.id = o.table_id
                                WHERE i.establishment_id = :establishmentId
                                  AND i.created_at >= :start AND i.created_at < :end
                                  AND i.ready_at IS NOT NULL
                                  AND i.status <> 'CANCELED'
                            ) parts
                            GROUP BY day, sector_id, product_id
                            """)
                    .param("establishmentId", establishmentId.value())
                    .param("zone", zone.getId())
                    .param("start", OffsetDateTime.ofInstant(from.atStartOfDay(zone).toInstant(), ZoneOffset.UTC))
                    .param("end", OffsetDateTime.ofInstant(until.atStartOfDay(zone).toInstant(), ZoneOffset.UTC))
                    .update();
        }
        jdbc.sql("""
                        INSERT INTO report_summary_state (establishment_id, summarized_until, updated_at)
                        VALUES (:establishmentId, :until, :now)
                        ON CONFLICT (establishment_id)
                        DO UPDATE SET summarized_until = EXCLUDED.summarized_until, updated_at = EXCLUDED.updated_at
                        """)
                .param("establishmentId", establishmentId.value())
                .param("until", Date.valueOf(until))
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .update();
    }

    @Override
    public void invalidate(EstablishmentId establishmentId) {
        jdbc.sql("DELETE FROM report_summary_state WHERE establishment_id = :establishmentId")
                .param("establishmentId", establishmentId.value())
                .update();
        jdbc.sql("DELETE FROM report_product_day WHERE establishment_id = :establishmentId")
                .param("establishmentId", establishmentId.value())
                .update();
    }
}
