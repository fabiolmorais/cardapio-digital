package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.KitchenReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.PrepAverage;
import br.com.cardapio.domain.report.model.PrepTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os tempos da cozinha em SQL nativo, só leitura e parametrizado (ADR 0013). A chegada à cozinha é a
 * confirmação do garçom, quando o pedido precisou dela; senão, o próprio pedido. O produto e a
 * categoria são os de hoje; o setor é o da mesa do pedido.
 */
@Component
class KitchenReportPersistenceAdapter implements KitchenReportPort {

    private final JdbcClient jdbc;

    KitchenReportPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PrepTime> prepTimes(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                    Optional<SectorId> sector, Optional<MenuCategoryId> category) {
        return jdbc.sql("""
                        SELECT i.product_id, p.name, c.name AS category,
                               CAST(extract(isodow FROM k.received AT TIME ZONE :zone) AS int) AS weekday,
                               CAST(extract(hour FROM k.received AT TIME ZONE :zone) AS int) AS hour,
                               greatest(0, extract(epoch FROM i.ready_at - k.received)) AS seconds
                        FROM order_item i
                        JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                        JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                        JOIN menu_category c ON c.id = p.category_id
                        CROSS JOIN LATERAL (
                            SELECT greatest(i.created_at, coalesce(o.approved_at, i.created_at)) AS received
                        ) k
                        WHERE i.establishment_id = :establishmentId
                          AND i.created_at >= :start AND i.created_at < :end
                          AND i.ready_at IS NOT NULL
                          AND i.status <> 'CANCELED'
                          AND (CAST(:sectorId AS uuid) IS NULL OR o.table_id IN (
                              SELECT dt.id FROM dining_table dt
                              WHERE dt.establishment_id = :establishmentId AND dt.sector_id = CAST(:sectorId AS uuid)))
                          AND (CAST(:categoryId AS uuid) IS NULL OR c.id = CAST(:categoryId AS uuid))
                        """)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("zone", zone.getId())
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .param("categoryId", category.map(MenuCategoryId::value).orElse(null), Types.OTHER)
                .query((rs, row) -> new PrepTime(new ProductId(rs.getObject("product_id", UUID.class)),
                        rs.getString("name"), rs.getString("category"), rs.getInt("weekday"), rs.getInt("hour"),
                        Duration.ofMillis(Math.round(rs.getDouble("seconds") * 1000))))
                .list();
    }

    @Override
    public List<PrepAverage> averages(EstablishmentId establishmentId, Instant start, Instant end,
                                      Optional<SectorId> sector) {
        return jdbc.sql("""
                        SELECT i.product_id, p.name, count(*) AS preparations,
                               avg(greatest(0, extract(epoch FROM i.ready_at
                                   - greatest(i.created_at, coalesce(o.approved_at, i.created_at))))) AS seconds
                        FROM order_item i
                        JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                        JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                        WHERE i.establishment_id = :establishmentId
                          AND i.created_at >= :start AND i.created_at < :end
                          AND i.ready_at IS NOT NULL
                          AND i.status <> 'CANCELED'
                          AND (CAST(:sectorId AS uuid) IS NULL OR o.table_id IN (
                              SELECT dt.id FROM dining_table dt
                              WHERE dt.establishment_id = :establishmentId AND dt.sector_id = CAST(:sectorId AS uuid)))
                        GROUP BY i.product_id, p.name
                        """)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .query((rs, row) -> new PrepAverage(new ProductId(rs.getObject("product_id", UUID.class)),
                        rs.getString("name"), rs.getInt("preparations"),
                        Duration.ofSeconds(Math.round(rs.getDouble("seconds")))))
                .list();
    }
}
