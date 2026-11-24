package br.com.cardapio.adapter.out.persistence.rating;

import br.com.cardapio.application.port.out.rating.RatingPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rating.model.RatingId;
import br.com.cardapio.domain.rating.model.RatingSummary;
import br.com.cardapio.domain.rating.model.RatingTag;
import br.com.cardapio.domain.rating.model.RatingView;
import br.com.cardapio.domain.rating.model.ServiceRating;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** As avaliações em SQL nativo e parametrizado, sempre por restaurante. */
@Component
class RatingPersistenceAdapter implements RatingPort {

    private static final String COLUMNS = """
            r.id, r.establishment_id, r.session_id, r.table_id, r.tab_id, r.waiter_id, r.stars, r.tag_speed,
            r.tag_friendliness, r.tag_food, r.tag_ambience, r.comment, r.created_at, r.google_clicked_at,
            r.resolved_at, r.resolved_by
            """;

    /** A avaliação com a mesa e o nome de quem atendeu, para a tela. */
    private static final String VIEW = "SELECT " + COLUMNS + """
            , dt.label AS table_label, u.name AS waiter_name
            FROM service_rating r
            JOIN dining_table dt ON dt.id = r.table_id AND dt.establishment_id = :establishmentId
            LEFT JOIN app_user u ON u.id = r.waiter_id
            """;

    private final JdbcClient jdbc;

    RatingPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static <T> T id(ResultSet rs, String column, java.util.function.Function<UUID, T> wrap)
            throws SQLException {
        UUID value = rs.getObject(column, UUID.class);
        return value == null ? null : wrap.apply(value);
    }

    private static final RowMapper<ServiceRating> RATING = (rs, row) -> {
        Set<RatingTag> tags = EnumSet.noneOf(RatingTag.class);
        if (rs.getBoolean("tag_speed")) tags.add(RatingTag.SPEED);
        if (rs.getBoolean("tag_friendliness")) tags.add(RatingTag.FRIENDLINESS);
        if (rs.getBoolean("tag_food")) tags.add(RatingTag.FOOD);
        if (rs.getBoolean("tag_ambience")) tags.add(RatingTag.AMBIENCE);
        return new ServiceRating(new RatingId(rs.getObject("id", UUID.class)),
                new EstablishmentId(rs.getObject("establishment_id", UUID.class)),
                new TableSessionId(rs.getObject("session_id", UUID.class)),
                new DiningTableId(rs.getObject("table_id", UUID.class)), id(rs, "tab_id", TabId::new),
                id(rs, "waiter_id", UserId::new), rs.getInt("stars"), tags, rs.getString("comment"),
                instant(rs, "created_at"), instant(rs, "google_clicked_at"), instant(rs, "resolved_at"),
                id(rs, "resolved_by", UserId::new));
    };

    private static final RowMapper<RatingView> RATING_VIEW = (rs, row) -> new RatingView(RATING.mapRow(rs, row),
            rs.getString("table_label"), rs.getString("waiter_name"));

    @Override
    public Visit visit(EstablishmentId establishmentId, DiningTableId tableId, Instant since) {
        // A comanda da visita: a fechada depois de o celular chegar (o fechamento encerra a sessão), ou a aberta.
        return jdbc.sql("""
                        SELECT (SELECT t.id FROM tab t
                                WHERE t.establishment_id = :establishmentId AND t.table_id = :tableId
                                  AND t.status = 'CLOSED' AND t.closed_at >= :since
                                ORDER BY t.closed_at DESC LIMIT 1) AS closed_tab,
                               (SELECT t.id FROM tab t
                                WHERE t.establishment_id = :establishmentId AND t.table_id = :tableId
                                  AND t.status IN ('OPEN', 'CLOSING')
                                LIMIT 1) AS open_tab,
                               EXISTS (SELECT 1 FROM call c
                                       WHERE c.establishment_id = :establishmentId AND c.table_id = :tableId
                                         AND c.type = 'BILL' AND c.status <> 'CANCELED'
                                         AND c.created_at >= :since) AS bill
                        """)
                .param("establishmentId", establishmentId.value())
                .param("tableId", tableId.value())
                .param("since", utc(since))
                .query((rs, row) -> {
                    TabId closed = id(rs, "closed_tab", TabId::new);
                    TabId open = id(rs, "open_tab", TabId::new);
                    return new Visit(Optional.ofNullable(closed != null ? closed : open), rs.getBoolean("bill"),
                            closed != null);
                })
                .single();
    }

    @Override
    public Optional<ServiceRating> findBySession(EstablishmentId establishmentId, TableSessionId sessionId) {
        return jdbc.sql("SELECT " + COLUMNS + """
                        FROM service_rating r
                        WHERE r.establishment_id = :establishmentId AND r.session_id = :sessionId
                        """)
                .param("establishmentId", establishmentId.value())
                .param("sessionId", sessionId.value())
                .query(RATING)
                .optional();
    }

    @Override
    public Optional<ServiceRating> find(EstablishmentId establishmentId, RatingId id) {
        return jdbc.sql("SELECT " + COLUMNS + """
                        FROM service_rating r
                        WHERE r.establishment_id = :establishmentId AND r.id = :id
                        """)
                .param("establishmentId", establishmentId.value())
                .param("id", id.value())
                .query(RATING)
                .optional();
    }

    @Override
    public boolean insert(ServiceRating r) {
        return jdbc.sql("""
                        INSERT INTO service_rating (id, establishment_id, session_id, table_id, tab_id, waiter_id, stars,
                                                    tag_speed, tag_friendliness, tag_food, tag_ambience, comment,
                                                    created_at)
                        VALUES (:id, :establishmentId, :sessionId, :tableId, :tabId, :waiterId, :stars, :speed,
                                :friendliness, :food, :ambience, :comment, :createdAt)
                        ON CONFLICT (session_id) DO NOTHING
                        """)
                .param("id", r.id().value())
                .param("establishmentId", r.establishmentId().value())
                .param("sessionId", r.sessionId().value())
                .param("tableId", r.tableId().value())
                .param("tabId", r.tabId() == null ? null : r.tabId().value())
                .param("waiterId", r.waiterId() == null ? null : r.waiterId().value())
                .param("stars", r.stars())
                .param("speed", r.tags().contains(RatingTag.SPEED))
                .param("friendliness", r.tags().contains(RatingTag.FRIENDLINESS))
                .param("food", r.tags().contains(RatingTag.FOOD))
                .param("ambience", r.tags().contains(RatingTag.AMBIENCE))
                .param("comment", r.comment())
                .param("createdAt", utc(r.createdAt()))
                .update() == 1;
    }

    @Override
    public void update(ServiceRating r) {
        jdbc.sql("""
                        UPDATE service_rating SET google_clicked_at = :google, resolved_at = :resolvedAt,
                                                  resolved_by = :resolvedBy
                        WHERE establishment_id = :establishmentId AND id = :id
                        """)
                .param("google", utc(r.googleClickedAt()))
                .param("resolvedAt", utc(r.resolvedAt()))
                .param("resolvedBy", r.resolvedBy() == null ? null : r.resolvedBy().value())
                .param("establishmentId", r.establishmentId().value())
                .param("id", r.id().value())
                .update();
    }

    @Override
    public ResultPage<RatingView> findBetween(EstablishmentId establishmentId, Instant start, Instant end,
                                              boolean onlyLow, PageQuery page) {
        record Row(RatingView view, long total) {
        }
        List<Row> rows = jdbc.sql(VIEW.replace("SELECT ", "SELECT count(*) OVER () AS total, ") + """
                        WHERE r.establishment_id = :establishmentId AND r.created_at >= :start AND r.created_at < :end
                          AND (NOT :onlyLow OR r.stars <= 2)
                        ORDER BY r.created_at DESC, r.id
                        LIMIT :size OFFSET :offset
                        """)
                .param("establishmentId", establishmentId.value())
                .param("start", utc(start))
                .param("end", utc(end))
                .param("onlyLow", onlyLow)
                .param("size", page.size())
                .param("offset", (long) page.page() * page.size())
                .query((rs, row) -> new Row(RATING_VIEW.mapRow(rs, row), rs.getLong("total")))
                .list();
        long total = rows.isEmpty() ? countBetween(establishmentId, start, end, onlyLow) : rows.getFirst().total();
        return new ResultPage<>(rows.stream().map(Row::view).toList(), page.page(), page.size(), total);
    }

    /** Uma página além do fim não traz linhas: o total vem à parte. */
    private long countBetween(EstablishmentId establishmentId, Instant start, Instant end, boolean onlyLow) {
        return jdbc.sql("""
                        SELECT count(*) FROM service_rating r
                        WHERE r.establishment_id = :establishmentId AND r.created_at >= :start AND r.created_at < :end
                          AND (NOT :onlyLow OR r.stars <= 2)
                        """)
                .param("establishmentId", establishmentId.value())
                .param("start", utc(start))
                .param("end", utc(end))
                .param("onlyLow", onlyLow)
                .query(Long.class)
                .single();
    }

    private static final RowMapper<RatingSummary> SUMMARY = (rs, row) -> {
        int count = rs.getInt("n");
        return new RatingSummary(count, count == 0 ? null : rs.getDouble("average"), rs.getInt("low"),
                rs.getInt("high"), rs.getInt("google"));
    };

    private static final String SUMMARY_COLUMNS = """
            SELECT count(*) AS n, avg(r.stars) AS average, count(*) FILTER (WHERE r.stars <= 2) AS low,
                   count(*) FILTER (WHERE r.stars >= 4) AS high,
                   count(*) FILTER (WHERE r.google_clicked_at IS NOT NULL) AS google
            FROM service_rating r
            """;

    @Override
    public RatingSummary summary(EstablishmentId establishmentId, Instant start, Instant end) {
        return jdbc.sql(SUMMARY_COLUMNS + """
                        WHERE r.establishment_id = :establishmentId AND r.created_at >= :start AND r.created_at < :end
                        """)
                .param("establishmentId", establishmentId.value())
                .param("start", utc(start))
                .param("end", utc(end))
                .query(SUMMARY)
                .single();
    }

    @Override
    public List<RatingView> openAlerts(EstablishmentId establishmentId, Instant start, Instant end) {
        return new ArrayList<>(jdbc.sql(VIEW + """
                        WHERE r.establishment_id = :establishmentId AND r.created_at >= :start AND r.created_at < :end
                          AND r.stars <= 2 AND r.resolved_at IS NULL
                        ORDER BY r.created_at DESC, r.id
                        LIMIT 20
                        """)
                .param("establishmentId", establishmentId.value())
                .param("start", utc(start))
                .param("end", utc(end))
                .query(RATING_VIEW)
                .list());
    }

    @Override
    public RatingSummary waiterSummary(EstablishmentId establishmentId, UserId waiterId, Instant start, Instant end) {
        return jdbc.sql(SUMMARY_COLUMNS + """
                        WHERE r.establishment_id = :establishmentId AND r.waiter_id = :waiterId
                          AND r.created_at >= :start AND r.created_at < :end
                        """)
                .param("establishmentId", establishmentId.value())
                .param("waiterId", waiterId.value())
                .param("start", utc(start))
                .param("end", utc(end))
                .query(SUMMARY)
                .single();
    }

    @Override
    public boolean waiterSeesAverage(EstablishmentId establishmentId) {
        return jdbc.sql("SELECT waiter_sees_average FROM rating_setting WHERE establishment_id = :establishmentId")
                .param("establishmentId", establishmentId.value())
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    @Override
    public void saveWaiterSeesAverage(EstablishmentId establishmentId, boolean visible, Instant now) {
        jdbc.sql("""
                        INSERT INTO rating_setting (establishment_id, waiter_sees_average, updated_at)
                        VALUES (:establishmentId, :visible, :now)
                        ON CONFLICT (establishment_id) DO UPDATE SET waiter_sees_average = EXCLUDED.waiter_sees_average,
                                                                      updated_at = EXCLUDED.updated_at
                        """)
                .param("establishmentId", establishmentId.value())
                .param("visible", visible)
                .param("now", utc(now))
                .update();
    }
}
