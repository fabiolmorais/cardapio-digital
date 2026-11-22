package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.TableReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.report.model.ForgottenTab;
import br.com.cardapio.domain.report.model.ForgottenTabs;
import br.com.cardapio.domain.report.model.OccupiedInterval;
import br.com.cardapio.domain.report.model.StayTotals;
import br.com.cardapio.domain.report.model.TableMoves;
import br.com.cardapio.domain.report.model.TableUsage;
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
 * As mesas e a ocupação em SQL nativo, só leitura e parametrizado (ADR 0013). O setor, quando vem, é o
 * da mesa (das mesas deste restaurante).
 */
@Component
class TableReportPersistenceAdapter implements TableReportPort {

    /**
     * A mesa {@code column} está no setor pedido. Sem setor, o trecho nem entra (ver o adaptador de
     * equipe: a forma "setor nulo ou mesa no setor" piora o plano genérico do PostgreSQL).
     */
    private static String inSector(String column, Optional<SectorId> sector) {
        return sector.isEmpty() ? " TRUE " : " " + column + " IN ("
                + "SELECT dt.id FROM dining_table dt WHERE dt.establishment_id = :establishmentId"
                + " AND dt.sector_id = CAST(:sectorId AS uuid)) ";
    }

    private final JdbcClient jdbc;

    TableReportPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private JdbcClient.StatementSpec sql(String sql, EstablishmentId establishmentId, Instant start, Instant end,
                                         Optional<SectorId> sector) {
        return jdbc.sql(sql)
                .param("establishmentId", establishmentId.value())
                .param("start", utc(start))
                .param("end", utc(end))
                .param("sectorId", sector.map(SectorId::value).orElse(null), Types.OTHER);
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Optional<Duration> seconds(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? Optional.empty() : Optional.of(Duration.ofSeconds(Math.round(value)));
    }

    @Override
    public StayTotals stays(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector) {
        return sql("""
                        SELECT count(*) AS tabs, coalesce(sum(t.total_cents), 0) AS revenue,
                               coalesce(sum(extract(epoch FROM t.closed_at - t.opened_at)), 0) AS occupied,
                               avg(extract(epoch FROM t.closed_at - t.opened_at)) AS stay,
                               avg(extract(epoch FROM t.closed_at - t.opened_at)) FILTER (WHERE r.id IS NOT NULL)
                                 AS rodizio,
                               avg(extract(epoch FROM t.closed_at - t.opened_at)) FILTER (WHERE r.id IS NULL)
                                 AS a_la_carte
                        FROM tab t
                        LEFT JOIN tab_rodizio r ON r.tab_id = t.id AND r.establishment_id = :establishmentId
                        WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                          AND t.closed_at >= :start AND t.closed_at < :end AND""" + inSector("t.table_id", sector),
                establishmentId, start, end, sector)
                .query((rs, row) -> new StayTotals(rs.getInt("tabs"), Money.ofCents(rs.getLong("revenue")),
                        Duration.ofSeconds(Math.round(rs.getDouble("occupied"))), seconds(rs, "stay"),
                        seconds(rs, "rodizio"), seconds(rs, "a_la_carte")))
                .single();
    }

    @Override
    public List<TableUsage> usage(EstablishmentId establishmentId, Instant start, Instant end,
                                  Optional<SectorId> sector) {
        return sql("""
                        SELECT dt.label, s.name AS sector, count(t.id) AS tabs,
                               coalesce(sum(t.total_cents), 0) AS revenue
                        FROM dining_table dt
                        JOIN sector s ON s.id = dt.sector_id AND s.establishment_id = :establishmentId
                        LEFT JOIN tab t ON t.table_id = dt.id AND t.establishment_id = :establishmentId
                          AND t.status = 'CLOSED' AND t.closed_at >= :start AND t.closed_at < :end
                        WHERE dt.establishment_id = :establishmentId AND dt.active
                          AND""" + inSector("dt.id", sector) + """
                        GROUP BY dt.id, dt.label, s.id, s.position, s.name
                        ORDER BY s.position, s.name, s.id, length(dt.label), dt.label, dt.id
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new TableUsage(rs.getString("label"), rs.getString("sector"), rs.getInt("tabs"),
                        Money.ofCents(rs.getLong("revenue"))))
                .list();
    }

    @Override
    public List<OccupiedInterval> occupied(EstablishmentId establishmentId, Instant start, Instant end, Instant now,
                                           Optional<SectorId> sector) {
        // Fechadas pelo índice do fechamento; abertas pelo das comandas abertas.
        return sql("""
                        SELECT t.table_id, t.opened_at, t.closed_at AS until FROM tab t
                        WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                          AND t.closed_at > :start AND t.opened_at < :end AND""" + inSector("t.table_id", sector) + """
                        UNION ALL
                        SELECT t.table_id, t.opened_at, CAST(:now AS timestamptz) AS until FROM tab t
                        WHERE t.establishment_id = :establishmentId AND t.status IN ('OPEN', 'CLOSING')
                          AND t.opened_at < :end AND""" + inSector("t.table_id", sector),
                establishmentId, start, end, sector)
                .param("now", utc(now))
                .query((rs, row) -> new OccupiedInterval(new DiningTableId(rs.getObject("table_id", UUID.class)),
                        rs.getObject("opened_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("until", OffsetDateTime.class).toInstant()))
                .list();
    }

    @Override
    public ForgottenTabs forgotten(EstablishmentId establishmentId, Instant start, Instant end, Instant now,
                                   Duration limit, Optional<SectorId> sector, int shown) {
        record Row(ForgottenTab tab, int total) {
        }
        List<Row> rows = sql("""
                        SELECT t.id, dt.label, t.opened_at, t.closed_at, count(*) OVER () AS total
                        FROM tab t
                        JOIN dining_table dt ON dt.id = t.table_id AND dt.establishment_id = :establishmentId
                        WHERE t.establishment_id = :establishmentId
                          AND t.opened_at >= :start AND t.opened_at < :end
                          AND ((t.status = 'CLOSED' AND t.closed_at > t.opened_at + make_interval(secs => :limit))
                               OR (t.status IN ('OPEN', 'CLOSING')
                                   AND t.opened_at < CAST(:now AS timestamptz) - make_interval(secs => :limit)))
                          AND""" + inSector("t.table_id", sector) + """
                        ORDER BY t.opened_at DESC, t.id
                        LIMIT :shown
                        """,
                establishmentId, start, end, sector)
                .param("now", utc(now))
                .param("limit", limit.toSeconds())
                .param("shown", shown)
                .query((rs, row) -> {
                    OffsetDateTime closed = rs.getObject("closed_at", OffsetDateTime.class);
                    return new Row(new ForgottenTab(new TabId(rs.getObject("id", UUID.class)), rs.getString("label"),
                            rs.getObject("opened_at", OffsetDateTime.class).toInstant(),
                            Optional.ofNullable(closed).map(OffsetDateTime::toInstant)), rs.getInt("total"));
                })
                .list();
        return new ForgottenTabs(rows.isEmpty() ? 0 : rows.getFirst().total(), rows.stream().map(Row::tab).toList());
    }

    @Override
    public TableMoves moves(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector) {
        return sql("""
                        SELECT count(*) FILTER (WHERE m.merged_tab_id IS NULL) AS moves,
                               count(*) FILTER (WHERE m.merged_tab_id IS NOT NULL) AS merges,
                               (SELECT dt.label FROM tab_move x
                                JOIN dining_table dt ON dt.id = x.from_table_id
                                  AND dt.establishment_id = :establishmentId
                                WHERE x.establishment_id = :establishmentId AND x.merged_tab_id IS NULL
                                  AND x.moved_at >= :start AND x.moved_at < :end
                                  AND""" + inSector("x.from_table_id", sector) + """
                                GROUP BY dt.id, dt.label
                                ORDER BY count(*) DESC, dt.label, dt.id
                                LIMIT 1) AS top_from,
                               (SELECT count(*) FROM tab_move x
                                WHERE x.establishment_id = :establishmentId AND x.merged_tab_id IS NULL
                                  AND x.moved_at >= :start AND x.moved_at < :end
                                  AND""" + inSector("x.from_table_id", sector) + """
                                GROUP BY x.from_table_id
                                ORDER BY count(*) DESC
                                LIMIT 1) AS top_from_moves
                        FROM tab_move m
                        WHERE m.establishment_id = :establishmentId
                          AND m.moved_at >= :start AND m.moved_at < :end
                          AND""" + inSector("m.from_table_id", sector),
                establishmentId, start, end, sector)
                .query((rs, row) -> new TableMoves(rs.getInt("moves"), rs.getInt("merges"),
                        Optional.ofNullable(rs.getString("top_from")), rs.getInt("top_from_moves")))
                .single();
    }
}
