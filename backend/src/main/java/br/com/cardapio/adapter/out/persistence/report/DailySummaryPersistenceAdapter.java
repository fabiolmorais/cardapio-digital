package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.DailySummaryPort;
import br.com.cardapio.application.port.out.report.SummaryTarget;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.report.model.DailySummarySettings;
import br.com.cardapio.domain.report.model.SummaryRecipient;
import br.com.cardapio.domain.shared.model.Email;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A configuração e os destinatários do resumo diário, em SQL parametrizado (ADR 0013). Sem linha em
 * {@code daily_summary_setting}, vale o padrão ({@link DailySummarySettings#DEFAULT}).
 */
@Component
class DailySummaryPersistenceAdapter implements DailySummaryPort {

    private static final String TARGET = """
            SELECT e.id, e.name, e.time_zone,
                   coalesce(s.email_enabled, true) AS email_enabled,
                   coalesce(s.push_enabled, true) AS push_enabled,
                   coalesce(s.send_hour, 2) AS send_hour,
                   s.last_sent_day
            FROM establishment e
            LEFT JOIN daily_summary_setting s ON s.establishment_id = e.id
            """;

    private static final RowMapper<SummaryTarget> TARGET_ROW = (rs, row) -> {
        Date last = rs.getDate("last_sent_day");
        return new SummaryTarget(new EstablishmentId(rs.getObject("id", UUID.class)), rs.getString("name"),
                ZoneId.of(rs.getString("time_zone")),
                new DailySummarySettings(rs.getBoolean("email_enabled"), rs.getBoolean("push_enabled"),
                        rs.getInt("send_hour")),
                Optional.ofNullable(last).map(Date::toLocalDate));
    };

    private final JdbcClient jdbc;

    DailySummaryPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<DailySummarySettings> settings(EstablishmentId establishmentId) {
        return jdbc.sql("""
                        SELECT email_enabled, push_enabled, send_hour FROM daily_summary_setting
                        WHERE establishment_id = :establishmentId
                        """)
                .param("establishmentId", establishmentId.value())
                .query((rs, row) -> new DailySummarySettings(rs.getBoolean("email_enabled"),
                        rs.getBoolean("push_enabled"), rs.getInt("send_hour")))
                .optional();
    }

    @Override
    public void saveSettings(EstablishmentId establishmentId, DailySummarySettings settings) {
        jdbc.sql("""
                        INSERT INTO daily_summary_setting (establishment_id, email_enabled, push_enabled, send_hour)
                        VALUES (:establishmentId, :email, :push, :hour)
                        ON CONFLICT (establishment_id) DO UPDATE
                        SET email_enabled = :email, push_enabled = :push, send_hour = :hour, updated_at = now()
                        """)
                .param("establishmentId", establishmentId.value())
                .param("email", settings.emailEnabled())
                .param("push", settings.pushEnabled())
                .param("hour", settings.sendHour())
                .update();
    }

    @Override
    public List<SummaryRecipient> recipients(EstablishmentId establishmentId) {
        return jdbc.sql("""
                        SELECT u.id, u.name, u.email, m.role, m.daily_summary
                        FROM membership m
                        JOIN app_user u ON u.id = m.user_id
                        WHERE m.establishment_id = :establishmentId
                          AND m.active AND u.active
                          AND m.role IN ('OWNER', 'MANAGER')
                        ORDER BY CASE m.role WHEN 'OWNER' THEN 0 ELSE 1 END, u.name, u.id
                        """)
                .param("establishmentId", establishmentId.value())
                .query((rs, row) -> new SummaryRecipient(new UserId(rs.getObject("id", UUID.class)),
                        rs.getString("name"), Role.valueOf(rs.getString("role")),
                        Optional.ofNullable(rs.getString("email")).map(Email::new), rs.getBoolean("daily_summary")))
                .list();
    }

    @Override
    public void setReceivers(EstablishmentId establishmentId, Set<UserId> receivers) {
        if (receivers.isEmpty()) {
            // Ninguém recebe (o IN de uma lista vazia não seria SQL válido).
            jdbc.sql("""
                            UPDATE membership SET daily_summary = false
                            WHERE establishment_id = :establishmentId AND role IN ('OWNER', 'MANAGER')
                            """)
                    .param("establishmentId", establishmentId.value())
                    .update();
            return;
        }
        jdbc.sql("""
                        UPDATE membership SET daily_summary = (user_id IN (:ids))
                        WHERE establishment_id = :establishmentId AND role IN ('OWNER', 'MANAGER')
                        """)
                .param("establishmentId", establishmentId.value())
                .param("ids", receivers.stream().map(UserId::value).toList())
                .update();
    }

    @Override
    public Optional<SummaryTarget> target(EstablishmentId establishmentId) {
        return jdbc.sql(TARGET + " WHERE e.id = :establishmentId")
                .param("establishmentId", establishmentId.value())
                .query(TARGET_ROW)
                .optional();
    }

    @Override
    public List<SummaryTarget> targets() {
        return jdbc.sql(TARGET + """
                         WHERE e.active
                           AND (s.establishment_id IS NULL OR s.email_enabled OR s.push_enabled)
                        """)
                .query(TARGET_ROW)
                .list();
    }

    @Override
    public boolean claim(EstablishmentId establishmentId, LocalDate day) {
        // Quem não tinha linha ganha a do padrão, já marcada; quem tinha só marca se o dia é novo.
        return jdbc.sql("""
                        INSERT INTO daily_summary_setting (establishment_id, last_sent_day)
                        VALUES (:establishmentId, :day)
                        ON CONFLICT (establishment_id) DO UPDATE SET last_sent_day = :day
                        WHERE daily_summary_setting.last_sent_day IS NULL
                           OR daily_summary_setting.last_sent_day < :day
                        """)
                .param("establishmentId", establishmentId.value())
                .param("day", day)
                .update() == 1;
    }
}
