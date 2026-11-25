package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.RatingReportPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.RatingComment;
import br.com.cardapio.domain.report.model.RatingDay;
import br.com.cardapio.domain.report.model.RatingStats;
import br.com.cardapio.domain.report.model.RatingStats.StarTotal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

/** As avaliações nos relatórios em SQL nativo e parametrizado, sempre por restaurante. */
@Component
class RatingReportPersistenceAdapter implements RatingReportPort {

    /** Os atalhos: o nome do enum e a coluna. */
    private static final Map<String, String> TAGS = new LinkedHashMap<>();

    static {
        TAGS.put("SPEED", "tag_speed");
        TAGS.put("FRIENDLINESS", "tag_friendliness");
        TAGS.put("FOOD", "tag_food");
        TAGS.put("AMBIENCE", "tag_ambience");
    }

    /** Com setor, só as mesas dele (sem setor, o trecho nem entra: o plano fica simples). */
    private static String inSector(String alias, Optional<SectorId> sector) {
        return sector.isEmpty() ? " TRUE " : " " + alias + ".table_id IN ("
                + "SELECT dt.id FROM dining_table dt WHERE dt.establishment_id = :establishmentId"
                + " AND dt.sector_id = CAST(:sectorId AS uuid)) ";
    }

    private final JdbcClient jdbc;

    RatingReportPersistenceAdapter(JdbcClient jdbc) {
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

    @Override
    public RatingStats stats(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                             int lunchUntil, Optional<SectorId> sector) {
        StringBuilder tags = new StringBuilder();
        TAGS.forEach((name, column) -> tags
                .append(", count(*) FILTER (WHERE stars >= 4 AND ").append(column).append(") AS praised_").append(column)
                .append(", count(*) FILTER (WHERE stars <= 3 AND ").append(column).append(") AS improve_").append(column));
        return sql("""
                        WITH r AS (
                            SELECT r.stars, r.google_clicked_at, r.tag_speed, r.tag_friendliness, r.tag_food,
                                   r.tag_ambience,
                                   extract(hour FROM r.created_at AT TIME ZONE :zone) < :lunchUntil AS lunch
                            FROM service_rating r
                            WHERE r.establishment_id = :establishmentId
                              AND r.created_at >= :start AND r.created_at < :end AND""" + inSector("r", sector) + """
                        )
                        SELECT count(*) AS n, coalesce(sum(stars), 0) AS star_sum,
                               count(*) FILTER (WHERE stars <= 2) AS low, count(*) FILTER (WHERE stars >= 4) AS high,
                               count(*) FILTER (WHERE google_clicked_at IS NOT NULL) AS google,
                               count(*) FILTER (WHERE stars = 1) AS s1, count(*) FILTER (WHERE stars = 2) AS s2,
                               count(*) FILTER (WHERE stars = 3) AS s3, count(*) FILTER (WHERE stars = 4) AS s4,
                               count(*) FILTER (WHERE stars = 5) AS s5,
                               count(*) FILTER (WHERE lunch) AS lunch_n,
                               coalesce(sum(stars) FILTER (WHERE lunch), 0) AS lunch_sum,
                               count(*) FILTER (WHERE NOT lunch) AS dinner_n,
                               coalesce(sum(stars) FILTER (WHERE NOT lunch), 0) AS dinner_sum
                        """ + tags + """
                               , (SELECT count(*) FROM tab t
                                  WHERE t.establishment_id = :establishmentId AND t.status = 'CLOSED'
                                    AND t.closed_at >= :start AND t.closed_at < :end AND""" + inSector("t", sector) + """
                                 ) AS tabs
                        FROM r
                        """, establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .param("lunchUntil", lunchUntil)
                .query((rs, row) -> {
                    Map<String, Integer> praised = new LinkedHashMap<>();
                    Map<String, Integer> improve = new LinkedHashMap<>();
                    for (var tag : TAGS.entrySet()) {
                        praised.put(tag.getKey(), rs.getInt("praised_" + tag.getValue()));
                        improve.put(tag.getKey(), rs.getInt("improve_" + tag.getValue()));
                    }
                    return new RatingStats(rs.getInt("n"), rs.getLong("star_sum"), rs.getInt("low"),
                            rs.getInt("high"), rs.getInt("google"),
                            List.of(rs.getInt("s1"), rs.getInt("s2"), rs.getInt("s3"), rs.getInt("s4"),
                                    rs.getInt("s5")),
                            new StarTotal(rs.getInt("lunch_n"), rs.getLong("lunch_sum")),
                            new StarTotal(rs.getInt("dinner_n"), rs.getLong("dinner_sum")), praised, improve,
                            rs.getInt("tabs"));
                })
                .single();
    }

    @Override
    public List<RatingDay> byDay(EstablishmentId establishmentId, Instant start, Instant end, ZoneId zone,
                                 Optional<SectorId> sector) {
        return sql("""
                        SELECT CAST(r.created_at AT TIME ZONE :zone AS date) AS day, count(*) AS n,
                               sum(r.stars) AS star_sum
                        FROM service_rating r
                        WHERE r.establishment_id = :establishmentId
                          AND r.created_at >= :start AND r.created_at < :end AND""" + inSector("r", sector) + """
                        GROUP BY 1
                        ORDER BY 1
                        """, establishmentId, start, end, sector)
                .param("zone", zone.getId())
                .query((rs, row) -> new RatingDay(rs.getObject("day", LocalDate.class), rs.getInt("n"),
                        rs.getLong("star_sum")))
                .list();
    }

    private static List<String> tags(ResultSet rs) throws SQLException {
        List<String> found = new ArrayList<>();
        for (var tag : TAGS.entrySet()) {
            if (rs.getBoolean(tag.getValue())) {
                found.add(tag.getKey());
            }
        }
        return found;
    }

    @Override
    public ResultPage<RatingComment> comments(EstablishmentId establishmentId, Instant start, Instant end,
                                              Optional<SectorId> sector, boolean onlyLow, PageQuery page) {
        record Row(RatingComment comment, long total) {
        }
        String where = """
                WHERE r.establishment_id = :establishmentId AND r.comment IS NOT NULL
                  AND r.created_at >= :start AND r.created_at < :end
                  AND (NOT :onlyLow OR r.stars <= 2) AND""" + inSector("r", sector);
        List<Row> rows = sql("""
                        SELECT count(*) OVER () AS total, dt.label, u.name AS waiter_name, r.stars, r.tag_speed,
                               r.tag_friendliness, r.tag_food, r.tag_ambience, r.comment, r.created_at
                        FROM service_rating r
                        JOIN dining_table dt ON dt.id = r.table_id AND dt.establishment_id = :establishmentId
                        LEFT JOIN app_user u ON u.id = r.waiter_id
                        """ + where + """
                        ORDER BY r.created_at DESC, r.id
                        LIMIT :size OFFSET :offset
                        """, establishmentId, start, end, sector)
                .param("onlyLow", onlyLow)
                .param("size", page.size())
                .param("offset", (long) page.page() * page.size())
                .query((rs, row) -> new Row(new RatingComment(rs.getString("label"), rs.getString("waiter_name"),
                        rs.getInt("stars"), tags(rs), rs.getString("comment"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()), rs.getLong("total")))
                .list();
        long total = rows.isEmpty()
                ? sql("SELECT count(*) FROM service_rating r " + where, establishmentId, start, end, sector)
                        .param("onlyLow", onlyLow).query(Long.class).single()
                : rows.getFirst().total();
        return new ResultPage<>(rows.stream().map(Row::comment).toList(), page.page(), page.size(), total);
    }
}
