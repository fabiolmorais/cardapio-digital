package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.TeamReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CallTotals;
import br.com.cardapio.domain.report.model.CallTypeCount;
import br.com.cardapio.domain.report.model.DuplicateWarnings;
import br.com.cardapio.domain.report.model.HourDuration;
import br.com.cardapio.domain.report.model.ShiftLoad;
import br.com.cardapio.domain.report.model.TableCalls;
import br.com.cardapio.domain.report.model.WaiterStats;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O atendimento e a equipe em SQL nativo, só leitura e parametrizado (ADR 0013). O setor, quando vem, é
 * o da mesa (das mesas deste restaurante).
 */
@Component
class TeamReportPersistenceAdapter implements TeamReportPort {

    /**
     * A mesa do registro {@code alias} está no setor pedido. Sem setor, o trecho nem entra: a forma "setor
     * nulo ou mesa no setor" faz o plano genérico do PostgreSQL (o das consultas repetidas) varrer demais.
     * O setor continua indo como parâmetro; só o trecho fixo muda.
     */
    private static String inSector(String alias, Optional<SectorId> sector) {
        return sector.isEmpty() ? " TRUE " : " " + alias + ".table_id IN ("
                + "SELECT dt.id FROM dining_table dt WHERE dt.establishment_id = :establishmentId"
                + " AND dt.sector_id = CAST(:sectorId AS uuid)) ";
    }

    private final JdbcClient jdbc;

    TeamReportPersistenceAdapter(JdbcClient jdbc) {
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
    public CallTotals calls(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector) {
        // Da conta pedida ao fechamento: a primeira comanda da mesa fechada depois do pedido, se já estava
        // aberta nele. A busca vai pelo índice tab_table_closed_idx (V47); conferir a abertura e o
        // restaurante fora do LATERAL mantém o plano bom também com os parâmetros da primeira execução.
        return sql("""
                        SELECT
                          (SELECT count(*) FROM call c
                           WHERE c.establishment_id = :establishmentId
                             AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                          ) AS calls,
                          (SELECT avg(extract(epoch FROM c.accepted_at - c.created_at)) FROM call c
                           WHERE c.establishment_id = :establishmentId AND c.accepted_at IS NOT NULL
                             AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                          ) AS response,
                          (SELECT avg(extract(epoch FROM c.done_at - c.created_at)) FROM call c
                           WHERE c.establishment_id = :establishmentId AND c.done_at IS NOT NULL
                             AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                          ) AS done,
                          (SELECT avg(extract(epoch FROM t.closed_at - c.created_at)) FROM call c
                           JOIN LATERAL (
                               SELECT t.closed_at, t.opened_at, t.establishment_id FROM tab t
                               WHERE t.table_id = c.table_id AND t.status = 'CLOSED'
                                 AND t.closed_at >= c.created_at
                               ORDER BY t.closed_at
                               LIMIT 1
                           ) t ON true
                           WHERE c.establishment_id = :establishmentId AND c.type = 'BILL'
                             AND t.establishment_id = :establishmentId AND t.opened_at <= c.created_at
                             AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                          ) AS bill_to_close
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new CallTotals(rs.getInt("calls"), seconds(rs, "response"), seconds(rs, "done"),
                        seconds(rs, "bill_to_close")))
                .single();
    }

    @Override
    public List<WaiterStats> waiters(EstablishmentId establishmentId, Instant start, Instant end,
                                     Optional<SectorId> sector) {
        return sql("""
                        WITH activity AS (
                            SELECT c.accepted_by AS user_id, 1 AS calls,
                                   extract(epoch FROM c.accepted_at - c.created_at) AS response,
                                   0 AS delivered, 0 AS closed, 0::bigint AS revenue, 0 AS orders, 0::bigint AS tips
                            FROM call c
                            WHERE c.establishment_id = :establishmentId AND c.accepted_by IS NOT NULL
                              AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                            UNION ALL
                            SELECT i.delivered_by, 0, NULL, i.quantity, 0, 0, 0, 0
                            FROM order_item i
                            JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                            WHERE i.establishment_id = :establishmentId AND i.delivered_by IS NOT NULL
                              AND i.delivered_at >= :start AND i.delivered_at < :end AND""" + inSector("o", sector) + """
                            UNION ALL
                            SELECT t.closed_by, 0, NULL, 0, 1, t.total_cents, 0, 0
                            FROM tab t
                            WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                              AND t.closed_by IS NOT NULL
                              AND t.closed_at >= :start AND t.closed_at < :end AND""" + inSector("t", sector) + """
                            UNION ALL
                            SELECT o.placed_by, 0, NULL, 0, 0, 0, 1, 0
                            FROM customer_order o
                            WHERE o.establishment_id = :establishmentId AND o.placed_by IS NOT NULL
                              AND o.status = 'SENT'
                              AND o.created_at >= :start AND o.created_at < :end AND""" + inSector("o", sector) + """
                            UNION ALL
                            SELECT t.service_waiter_id, 0, NULL, 0, 0, 0, 0, t.service_cents
                            FROM tab t
                            WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                              AND t.service_destination = 'WAITER' AND t.service_waiter_id IS NOT NULL
                              AND t.closed_at >= :start AND t.closed_at < :end AND""" + inSector("t", sector) + """
                        )
                        SELECT a.user_id, u.name, sum(a.calls) AS calls, avg(a.response) AS response,
                               sum(a.delivered) AS delivered, sum(a.closed) AS closed, sum(a.revenue) AS revenue,
                               sum(a.orders) AS orders, coalesce(sum(a.tips), 0) AS tips
                        FROM activity a
                        JOIN app_user u ON u.id = a.user_id
                        GROUP BY a.user_id, u.name
                        ORDER BY calls DESC, delivered DESC, u.name, a.user_id
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new WaiterStats(new UserId(rs.getObject("user_id", UUID.class)),
                        rs.getString("name"), rs.getInt("calls"), seconds(rs, "response"), rs.getInt("delivered"),
                        rs.getInt("closed"), Money.ofCents(rs.getLong("revenue")), rs.getInt("orders"),
                        Money.ofCents(rs.getLong("tips"))))
                .list();
    }

    @Override
    public List<HourDuration> responseByHour(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                             Optional<SectorId> sector) {
        return sql("""
                        SELECT CAST(extract(hour FROM c.created_at AT TIME ZONE :zone) AS int) AS hour,
                               avg(extract(epoch FROM c.accepted_at - c.created_at)) AS response, count(*) AS calls
                        FROM call c
                        WHERE c.establishment_id = :establishmentId AND c.accepted_at IS NOT NULL
                          AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                        GROUP BY 1
                        ORDER BY 1
                        """,
                establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .query((rs, row) -> new HourDuration(rs.getInt("hour"), seconds(rs, "response").orElse(Duration.ZERO),
                        rs.getInt("calls")))
                .list();
    }

    @Override
    public List<CallTypeCount> byType(EstablishmentId establishmentId, Instant start, Instant end,
                                      Optional<SectorId> sector) {
        return sql("""
                        SELECT c.type, count(*) AS calls, count(*) FILTER (WHERE c.status = 'CANCELED') AS canceled
                        FROM call c
                        WHERE c.establishment_id = :establishmentId
                          AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                        GROUP BY c.type
                        ORDER BY calls DESC, c.type
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new CallTypeCount(CallType.valueOf(rs.getString("type")), rs.getInt("calls"),
                        rs.getInt("canceled")))
                .list();
    }

    @Override
    public List<TableCalls> tables(EstablishmentId establishmentId, Instant start, Instant end,
                                   Optional<SectorId> sector) {
        return sql("""
                        SELECT dt.label, s.name AS sector, coalesce(c.calls, 0) AS calls, coalesce(t.tabs, 0) AS tabs
                        FROM dining_table dt
                        JOIN sector s ON s.id = dt.sector_id
                        LEFT JOIN (SELECT c.table_id, count(*) AS calls FROM call c
                                   WHERE c.establishment_id = :establishmentId
                                     AND c.created_at >= :start AND c.created_at < :end
                                   GROUP BY c.table_id) c ON c.table_id = dt.id
                        LEFT JOIN (SELECT t.table_id, count(*) AS tabs FROM tab t
                                   WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                                     AND t.closed_at >= :start AND t.closed_at < :end
                                   GROUP BY t.table_id) t ON t.table_id = dt.id
                        WHERE dt.establishment_id = :establishmentId AND""" + (sector.isEmpty() ? " TRUE"
                        : " dt.sector_id = CAST(:sectorId AS uuid)"),
                establishmentId, start, end, sector)
                .query((rs, row) -> new TableCalls(rs.getString("label"), rs.getString("sector"), rs.getInt("calls"),
                        rs.getInt("tabs")))
                .list();
    }

    @Override
    public DuplicateWarnings duplicateWarnings(EstablishmentId establishmentId, Instant start, Instant end,
                                               Optional<SectorId> sector) {
        // O aviso guarda a comanda sem chave estrangeira (a do envio recusado pode não existir): o setor só
        // filtra os avisos cuja comanda existe.
        return sql("""
                        SELECT count(*) AS shown, count(w.confirmed_at) AS confirmed
                        FROM duplicate_warning w
                        LEFT JOIN tab t ON t.id = w.tab_id AND t.establishment_id = :establishmentId
                        WHERE w.establishment_id = :establishmentId
                          AND w.warned_at >= :start AND w.warned_at < :end
                          AND""" + inSector("t", sector),
                establishmentId, start, end, sector)
                .query((rs, row) -> new DuplicateWarnings(rs.getInt("shown"), rs.getInt("confirmed")))
                .single();
    }

    @Override
    public List<ShiftLoad> shiftLoads(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                      Optional<SectorId> sector) {
        // As pessoas do salão (garçons e gerentes que entram pelo PIN) com o turno aberto em algum momento da
        // hora. O turno ainda aberto conta até agora.
        return sql("""
                        WITH slots AS (
                            SELECT date_trunc('hour', c.created_at AT TIME ZONE :zone) AS slot, count(*) AS calls
                            FROM call c
                            WHERE c.establishment_id = :establishmentId
                              AND c.created_at >= :start AND c.created_at < :end AND""" + inSector("c", sector) + """
                            GROUP BY 1
                        )
                        SELECT CAST(s.slot AS date) AS day, CAST(extract(hour FROM s.slot) AS int) AS hour, s.calls,
                               (SELECT count(DISTINCT sh.user_id) FROM staff_shift sh
                                JOIN membership m ON m.user_id = sh.user_id
                                                 AND m.establishment_id = sh.establishment_id
                                WHERE sh.establishment_id = :establishmentId
                                  AND m.role IN ('STAFF', 'MANAGER')
                                  AND sh.started_at < (s.slot + interval '1 hour') AT TIME ZONE :zone
                                  AND coalesce(sh.ended_at, now()) > s.slot AT TIME ZONE :zone) AS waiters
                        FROM slots s
                        """,
                establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .query((rs, row) -> new ShiftLoad(rs.getObject("day", LocalDate.class), rs.getInt("hour"),
                        rs.getInt("calls"), rs.getInt("waiters")))
                .list();
    }
}
