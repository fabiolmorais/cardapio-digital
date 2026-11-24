package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.PhoneRodizioReportPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.CardChange;
import br.com.cardapio.domain.report.model.CardTrack;
import br.com.cardapio.domain.report.model.DailyOrigin;
import br.com.cardapio.domain.report.model.PhoneFunnel;
import br.com.cardapio.domain.report.model.PhoneStats;
import br.com.cardapio.domain.report.model.PresenceCount;
import br.com.cardapio.domain.report.model.RodizioItem;
import br.com.cardapio.domain.report.model.RodizioTotals;
import br.com.cardapio.domain.report.model.TierCount;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pedido pelo celular e o rodízio em SQL nativo, só leitura e parametrizado (ADR 0013). O setor,
 * quando vem, é o da mesa (das mesas deste restaurante).
 *
 * <p>O celular entra na comanda pela mesa e pelo horário: a sessão é da comanda da mesa que fechou
 * primeiro depois de o celular escanear, se ela já estava aberta enquanto o celular estava ativo. A
 * busca vai pelo índice {@code tab_table_closed_idx} (V47).
 */
@Component
class PhoneRodizioReportPersistenceAdapter implements PhoneRodizioReportPort {

    /** A mesa {@code column} está no setor pedido; sem setor, o trecho nem entra (ver o adaptador de equipe). */
    private static String inSector(String column, Optional<SectorId> sector) {
        return sector.isEmpty() ? " TRUE " : " " + column + " IN ("
                + "SELECT dt.id FROM dining_table dt WHERE dt.establishment_id = :establishmentId"
                + " AND dt.sector_id = CAST(:sectorId AS uuid)) ";
    }

    /** A comanda fechada da mesa da sessão {@code s} (alias {@code t}), quando a sessão foi dela. */
    private static final String SESSION_TAB = """
            LEFT JOIN LATERAL (
                SELECT t.id, t.opened_at, t.establishment_id FROM tab t
                WHERE t.table_id = s.table_id AND t.status = 'CLOSED' AND t.closed_at >= s.created_at
                ORDER BY t.closed_at
                LIMIT 1
            ) t ON t.establishment_id = :establishmentId AND t.opened_at <= s.last_seen_at
            """;

    /** O rodízio (alias {@code r}) das comandas fechadas no período (alias {@code t}). */
    private static String rodizioTabs(Optional<SectorId> sector) {
        return " " + """
                FROM tab_rodizio r
                JOIN tab t ON t.id = r.tab_id AND t.establishment_id = :establishmentId
                WHERE r.establishment_id = :establishmentId AND t.status = 'CLOSED'
                  AND t.closed_at >= :start AND t.closed_at < :end AND""" + inSector("t.table_id", sector);
    }

    private final JdbcClient jdbc;

    PhoneRodizioReportPersistenceAdapter(JdbcClient jdbc) {
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

    private static Optional<Double> ratio(int count, int of) {
        return of == 0 ? Optional.empty() : Optional.of((double) count / of);
    }

    @Override
    public List<DailyOrigin> originByDay(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                         Optional<SectorId> sector) {
        return sql("""
                        SELECT CAST(o.created_at AT TIME ZONE :zone AS date) AS day,
                               count(*) FILTER (WHERE o.session_id IS NOT NULL) AS phone,
                               count(*) FILTER (WHERE o.placed_by IS NOT NULL) AS staff
                        FROM customer_order o
                        WHERE o.establishment_id = :establishmentId AND o.status = 'SENT'
                          AND o.created_at >= :start AND o.created_at < :end
                          AND""" + inSector("o.table_id", sector) + """
                        GROUP BY 1
                        ORDER BY 1
                        """,
                establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .query((rs, row) -> new DailyOrigin(rs.getObject("day", LocalDate.class), rs.getInt("phone"),
                        rs.getInt("staff")))
                .list();
    }

    @Override
    public PhoneFunnel funnel(EstablishmentId establishmentId, Instant start, Instant end,
                              Optional<SectorId> sector) {
        // As sessões de até um dia antes: o celular pode ter escaneado antes da meia-noite.
        return sql("""
                        WITH tabs AS (
                            SELECT t.id FROM tab t
                            WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                              AND t.closed_at >= :start AND t.closed_at < :end
                              AND""" + inSector("t.table_id", sector) + """
                        ), scanned AS (
                            SELECT DISTINCT t.id FROM table_session s
                        """ + SESSION_TAB + """
                            WHERE s.establishment_id = :establishmentId
                              AND s.created_at >= CAST(:start AS timestamptz) - interval '1 day'
                              AND s.created_at < :end AND t.id IS NOT NULL
                              AND""" + inSector("s.table_id", sector) + """
                        ), phone AS (
                            SELECT tabs.id FROM tabs
                            WHERE EXISTS (SELECT 1 FROM customer_order o
                                          WHERE o.tab_id = tabs.id AND o.establishment_id = :establishmentId
                                            AND o.status = 'SENT' AND o.session_id IS NOT NULL)
                        )
                        SELECT (SELECT count(*) FROM tabs) AS tabs,
                               (SELECT count(*) FROM tabs
                                WHERE tabs.id IN (SELECT id FROM scanned) OR tabs.id IN (SELECT id FROM phone))
                                 AS scanned,
                               (SELECT count(*) FROM phone) AS phone
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new PhoneFunnel(rs.getInt("tabs"), rs.getInt("scanned"), rs.getInt("phone")))
                .single();
    }

    @Override
    public PhoneStats phones(EstablishmentId establishmentId, Instant start, Instant end, Instant now, ZoneId zone,
                             Optional<SectorId> sector) {
        record Row(int withTab, int tabs, int ordering, Optional<Duration> scanToOrder, int noOrder, int rejected,
                   int blocked) {
        }
        Row r = sql("""
                        WITH sessions AS (
                            SELECT s.id, s.table_id, s.created_at, s.last_seen_at, t.id AS tab_id
                            FROM table_session s
                        """ + SESSION_TAB + """
                            WHERE s.establishment_id = :establishmentId
                              AND s.created_at >= :start AND s.created_at < :end
                              AND""" + inSector("s.table_id", sector) + """
                        ), orders AS (
                            SELECT o.session_id, min(o.created_at) FILTER (WHERE o.status = 'SENT') AS first_order
                            FROM customer_order o
                            WHERE o.establishment_id = :establishmentId AND o.session_id IS NOT NULL
                              AND o.created_at >= :start
                            GROUP BY o.session_id
                        ), open_tabs AS (
                            SELECT ot.table_id, ot.opened_at FROM tab ot
                            WHERE ot.establishment_id = :establishmentId AND ot.status IN ('OPEN', 'CLOSING')
                        )
                        SELECT count(*) FILTER (WHERE m.tab_id IS NOT NULL) AS with_tab,
                               count(DISTINCT m.tab_id) AS tabs,
                               count(*) FILTER (WHERE m.tab_id IS NOT NULL AND o.first_order IS NOT NULL)
                                 AS ordering,
                               avg(extract(epoch FROM o.first_order - m.created_at)) AS scan_to_order,
                               count(DISTINCT CAST(m.table_id AS text) || '/'
                                     || CAST(CAST(m.created_at AT TIME ZONE :zone AS date) AS text))
                                 FILTER (WHERE m.tab_id IS NULL AND o.session_id IS NULL
                                           AND m.last_seen_at < CAST(:now AS timestamptz) - interval '30 minutes'
                                           AND NOT EXISTS (SELECT 1 FROM open_tabs x
                                                           WHERE x.table_id = m.table_id
                                                             AND x.opened_at <= m.last_seen_at))
                                 AS no_order,
                               (SELECT count(*) FROM customer_order r
                                WHERE r.establishment_id = :establishmentId AND r.status = 'REJECTED'
                                  AND r.created_at >= :start AND r.created_at < :end
                                  AND""" + inSector("r.table_id", sector) + """
                               ) AS rejected,
                               (SELECT count(*) FROM table_session b
                                WHERE b.establishment_id = :establishmentId
                                  AND b.blocked_at >= :start AND b.blocked_at < :end
                                  AND""" + inSector("b.table_id", sector) + """
                               ) AS blocked
                        FROM sessions m
                        LEFT JOIN orders o ON o.session_id = m.id
                        """,
                establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .param("now", utc(now))
                .query((rs, row) -> new Row(rs.getInt("with_tab"), rs.getInt("tabs"), rs.getInt("ordering"),
                        seconds(rs, "scan_to_order"), rs.getInt("no_order"), rs.getInt("rejected"),
                        rs.getInt("blocked")))
                .single();
        return new PhoneStats(ratio(r.withTab(), r.tabs()), ratio(r.ordering(), r.tabs()), r.scanToOrder(),
                r.noOrder(), r.rejected(), r.blocked());
    }

    @Override
    public List<PresenceCount> presence(EstablishmentId establishmentId, Instant start, Instant end,
                                        Optional<SectorId> sector) {
        return sql("""
                        SELECT s.presence_method, count(*) AS sessions
                        FROM table_session s
                        WHERE s.establishment_id = :establishmentId AND s.presence_method IS NOT NULL
                          AND s.presence_confirmed_at >= :start AND s.presence_confirmed_at < :end
                          AND""" + inSector("s.table_id", sector) + """
                        GROUP BY 1
                        ORDER BY 2 DESC, 1
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new PresenceCount(PresenceMethod.valueOf(rs.getString("presence_method")),
                        rs.getInt("sessions")))
                .list();
    }

    @Override
    public RodizioTotals rodizio(EstablishmentId establishmentId, Instant start, Instant end,
                                 Optional<SectorId> sector) {
        return sql("""
                        WITH r AS (
                            SELECT r.id, r.started_at, r.ends_at, t.id AS tab_id, t.closed_at, t.total_cents,
                                   (SELECT coalesce(sum(g.quantity), 0) FROM tab_rodizio_guest g
                                    WHERE g.tab_rodizio_id = r.id AND g.establishment_id = :establishmentId)
                                     AS people,
                                   (SELECT coalesce(sum(g.quantity * g.unit_price_cents), 0) FROM tab_rodizio_guest g
                                    WHERE g.tab_rodizio_id = r.id AND g.establishment_id = :establishmentId)
                                     AS value
                        """ + rodizioTabs(sector) + """
                        )
                        SELECT count(*) AS tabs,
                               coalesce(sum(r.people), 0) AS people,
                               coalesce(sum(r.value), 0) AS revenue,
                               coalesce(sum(greatest(r.total_cents - r.value, 0)), 0) AS extra,
                               (SELECT count(*) FROM customer_order o
                                WHERE o.establishment_id = :establishmentId AND o.status <> 'REJECTED'
                                  AND o.tab_id IN (SELECT tab_id FROM r)
                                  AND EXISTS (SELECT 1 FROM order_item i
                                              WHERE i.order_id = o.id AND i.tab_rodizio_id IN (SELECT id FROM r)))
                                 AS rounds,
                               (SELECT coalesce(sum(i.quantity), 0) FROM order_item i
                                JOIN customer_order o ON o.id = i.order_id AND o.status <> 'REJECTED'
                                WHERE i.establishment_id = :establishmentId AND i.status <> 'CANCELED'
                                  AND i.tab_rodizio_id IN (SELECT id FROM r)) AS items,
                               avg(extract(epoch FROM r.closed_at - r.started_at)) AS time,
                               count(r.ends_at) AS with_limit,
                               count(*) FILTER (WHERE r.ends_at IS NOT NULL AND r.closed_at >= r.ends_at) AS reached
                        FROM r
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new RodizioTotals(rs.getInt("tabs"), rs.getInt("people"),
                        Money.ofCents(rs.getLong("revenue")), Money.ofCents(rs.getLong("extra")), rs.getInt("rounds"),
                        rs.getInt("items"), seconds(rs, "time"), rs.getInt("with_limit"), rs.getInt("reached")))
                .single();
    }

    @Override
    public List<TierCount> tiers(EstablishmentId establishmentId, Instant start, Instant end,
                                 Optional<SectorId> sector) {
        return sql("""
                        SELECT g.tier_name, sum(g.quantity) AS people
                        FROM tab_rodizio_guest g
                        WHERE g.establishment_id = :establishmentId
                          AND g.tab_rodizio_id IN (SELECT r.id""" + rodizioTabs(sector) + """
                          )
                        GROUP BY g.tier_name
                        HAVING sum(g.quantity) > 0
                        ORDER BY 2 DESC, 1
                        """,
                establishmentId, start, end, sector)
                .query((rs, row) -> new TierCount(rs.getString("tier_name"), rs.getInt("people")))
                .list();
    }

    @Override
    public List<RodizioItem> topItems(EstablishmentId establishmentId, Instant start, Instant end,
                                      Optional<SectorId> sector, Optional<RodizioType> type, int limit) {
        String ofType = type.isPresent() ? " AND r.rodizio_type = :type " : "";
        var spec = sql("""
                        SELECT p.name, sum(i.quantity) AS quantity
                        FROM order_item i
                        JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                          AND o.status = 'SENT'
                        JOIN product p ON p.id = i.product_id AND p.establishment_id = :establishmentId
                        WHERE i.establishment_id = :establishmentId AND i.status <> 'CANCELED'
                          AND i.tab_rodizio_id IN (SELECT r.id""" + rodizioTabs(sector) + ofType + """
                          )
                        GROUP BY p.id, p.name
                        ORDER BY quantity DESC, p.name, p.id
                        LIMIT :limit
                        """,
                establishmentId, start, end, sector)
                .param("limit", limit);
        if (type.isPresent()) {
            spec = spec.param("type", type.get().name());
        }
        return spec.query((rs, row) -> new RodizioItem(rs.getString("name"), rs.getInt("quantity"))).list();
    }

    @Override
    public List<CardTrack> cardTracks(EstablishmentId establishmentId, Instant start, Instant end, Instant since,
                                      Optional<SectorId> sector) {
        record Row(UUID id, Instant started, Instant closed, String status, Instant changed) {
        }
        List<Row> rows = sql("""
                        SELECT r.id, r.started_at, t.closed_at, c.status, c.changed_at
                        FROM tab_rodizio r
                        JOIN tab t ON t.id = r.tab_id AND t.establishment_id = :establishmentId
                        LEFT JOIN rodizio_card_change c ON c.tab_rodizio_id = r.id
                          AND c.establishment_id = :establishmentId
                        WHERE r.establishment_id = :establishmentId AND r.rodizio_type = 'PASSAGEM'
                          AND r.started_at >= :since AND t.status = 'CLOSED'
                          AND t.closed_at >= :start AND t.closed_at < :end
                          AND""" + inSector("t.table_id", sector) + """
                        ORDER BY r.id, c.changed_at
                        """,
                establishmentId, start, end, sector)
                .param("since", utc(since))
                .query((rs, row) -> {
                    OffsetDateTime changed = rs.getObject("changed_at", OffsetDateTime.class);
                    return new Row(rs.getObject("id", UUID.class),
                            rs.getObject("started_at", OffsetDateTime.class).toInstant(),
                            rs.getObject("closed_at", OffsetDateTime.class).toInstant(), rs.getString("status"),
                            changed == null ? null : changed.toInstant());
                })
                .list();
        Map<UUID, List<Row>> byRodizio = new LinkedHashMap<>();
        rows.forEach(row -> byRodizio.computeIfAbsent(row.id(), k -> new ArrayList<>()).add(row));
        return byRodizio.values().stream()
                .map(group -> new CardTrack(group.getFirst().started(), group.getFirst().closed(),
                        group.stream().filter(row -> row.changed() != null)
                                .map(row -> new CardChange(TabRodizioStatus.valueOf(row.status()), row.changed()))
                                .toList()))
                .toList();
    }
}
