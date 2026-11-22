package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.OperationReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CanceledProductCount;
import br.com.cardapio.domain.report.model.OperationTotals;
import br.com.cardapio.domain.report.model.WaiterCalls;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os números de operação em SQL nativo, só leitura e parametrizado (ADR 0013). O setor, quando vem, é
 * o da mesa (das mesas deste restaurante).
 */
@Component
class OperationReportPersistenceAdapter implements OperationReportPort {

    /** A mesa do registro {@code alias} está no setor pedido (ou não há setor). */
    private static String inSector(String alias) {
        return " (CAST(:sectorId AS uuid) IS NULL OR " + alias + ".table_id IN ("
                + "SELECT dt.id FROM dining_table dt WHERE dt.establishment_id = :establishmentId"
                + " AND dt.sector_id = CAST(:sectorId AS uuid))) ";
    }

    private final JdbcClient jdbc;

    OperationReportPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private JdbcClient.StatementSpec sql(String sql, EstablishmentId establishmentId, Instant start, Instant end,
                                         Optional<SectorId> sector) {
        return jdbc.sql(sql)
                .param("establishmentId", establishmentId.value())
                .param("start", OffsetDateTime.ofInstant(start, ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(end, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER);
    }

    private static Optional<Duration> seconds(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? Optional.empty() : Optional.of(Duration.ofSeconds(Math.round(value)));
    }

    @Override
    public OperationTotals totals(EstablishmentId establishmentId, Instant start, Instant end,
                                  Optional<SectorId> sector) {
        return sql("""
                        SELECT
                          (SELECT avg(extract(epoch FROM i.delivered_at
                                      - greatest(i.created_at, coalesce(o.approved_at, i.created_at))))
                           FROM order_item i
                           JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                           WHERE i.establishment_id = :establishmentId
                             AND i.created_at >= :start AND i.created_at < :end
                             AND i.status = 'DELIVERED' AND i.delivered_at IS NOT NULL
                             AND""" + inSector("o") + """
                          ) AS to_table,
                          (SELECT count(*) FROM customer_order o
                           WHERE o.establishment_id = :establishmentId AND o.status = 'SENT'
                             AND o.created_at >= :start AND o.created_at < :end AND o.session_id IS NOT NULL
                             AND""" + inSector("o") + """
                          ) AS phone_orders,
                          (SELECT count(*) FROM customer_order o
                           WHERE o.establishment_id = :establishmentId AND o.status = 'SENT'
                             AND o.created_at >= :start AND o.created_at < :end AND o.placed_by IS NOT NULL
                             AND""" + inSector("o") + """
                          ) AS staff_orders,
                          (SELECT avg(extract(epoch FROM c.accepted_at - c.created_at)) FROM call c
                           WHERE c.establishment_id = :establishmentId
                             AND c.created_at >= :start AND c.created_at < :end AND c.accepted_at IS NOT NULL
                             AND""" + inSector("c") + """
                          ) AS call_response,
                          (SELECT avg(extract(epoch FROM t.closed_at - t.opened_at)) FROM tab t
                           WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                             AND t.closed_at >= :start AND t.closed_at < :end
                             AND""" + inSector("t") + """
                          ) AS stay,
                          (SELECT coalesce(sum(i.unit_price_cents * i.quantity), 0)
                           FROM order_item i
                           JOIN customer_order o ON o.id = i.order_id
                           JOIN tab t ON t.id = o.tab_id
                           WHERE i.establishment_id = :establishmentId AND t.establishment_id = :establishmentId
                             AND t.status = 'CLOSED' AND t.closed_at >= :start AND t.closed_at < :end
                             AND i.status = 'CANCELED'
                             AND""" + inSector("t") + """
                          ) AS canceled
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new OperationTotals(seconds(rs, "to_table"), rs.getInt("phone_orders"),
                        rs.getInt("staff_orders"), seconds(rs, "call_response"), seconds(rs, "stay"),
                        Money.ofCents(rs.getLong("canceled"))))
                .single();
    }

    @Override
    public List<WaiterCalls> waiters(EstablishmentId establishmentId, Instant start, Instant end,
                                     Optional<SectorId> sector, int limit) {
        return sql("""
                        SELECT u.id, u.name, count(*) AS calls,
                               avg(extract(epoch FROM c.accepted_at - c.created_at)) AS response
                        FROM call c
                        JOIN app_user u ON u.id = c.accepted_by
                        WHERE c.establishment_id = :establishmentId
                          AND c.created_at >= :start AND c.created_at < :end AND c.accepted_at IS NOT NULL
                          AND""" + inSector("c") + """
                        GROUP BY u.id, u.name
                        ORDER BY calls DESC, response, u.name, u.id
                        LIMIT :limit
                        """,
                establishmentId, start, end, sector)
                .param("limit", limit)
                .query((rs, row) -> new WaiterCalls(new UserId(rs.getObject("id", UUID.class)), rs.getString("name"),
                        rs.getInt("calls"), seconds(rs, "response").orElse(Duration.ZERO)))
                .list();
    }

    @Override
    public List<String> forgottenTabs(EstablishmentId establishmentId, Instant openedBefore,
                                      Optional<SectorId> sector) {
        return jdbc.sql("""
                        SELECT dt.label FROM tab t
                        JOIN dining_table dt ON dt.id = t.table_id
                        WHERE t.establishment_id = :establishmentId AND t.status = 'OPEN'
                          AND t.opened_at < :before
                          AND""" + inSector("t") + """
                        ORDER BY t.opened_at, dt.label
                        """)
                .param("establishmentId", establishmentId.value())
                .param("before", OffsetDateTime.ofInstant(openedBefore, ZoneOffset.UTC))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER)
                .query(String.class)
                .list();
    }

    @Override
    public List<CanceledProductCount> canceled(EstablishmentId establishmentId, Instant start, Instant end,
                                               Optional<SectorId> sector) {
        return sql("""
                        SELECT p.name, nullif(btrim(i.cancel_reason), '') AS reason, sum(i.quantity) AS times
                        FROM order_item i
                        JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                        JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                        WHERE i.establishment_id = :establishmentId AND i.status = 'CANCELED'
                          AND i.canceled_at >= :start AND i.canceled_at < :end
                          AND""" + inSector("o") + """
                        GROUP BY p.name, nullif(btrim(i.cancel_reason), '')
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new CanceledProductCount(rs.getString("name"), rs.getString("reason"),
                        rs.getInt("times")))
                .list();
    }
}
