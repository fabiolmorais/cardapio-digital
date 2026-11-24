package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.ReportAlertPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.report.model.AlertFinding;
import br.com.cardapio.domain.report.model.AlertSettings;
import br.com.cardapio.domain.report.model.AlertType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os alertas em tempo real em SQL nativo e parametrizado, sempre por restaurante. Os atrasos usam os
 * índices das filas (itens a caminho, chamados abertos, comandas abertas).
 */
@Component
class ReportAlertPersistenceAdapter implements ReportAlertPort {

    private final JdbcClient jdbc;

    ReportAlertPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Optional<AlertSettings> settings(EstablishmentId establishmentId) {
        return jdbc.sql("""
                        SELECT kitchen_enabled, kitchen_minutes, call_enabled, call_minutes, tab_enabled, tab_hours
                        FROM report_alert_setting WHERE establishment_id = :establishmentId
                        """)
                .param("establishmentId", establishmentId.value())
                .query((rs, row) -> new AlertSettings(rs.getBoolean("kitchen_enabled"), rs.getInt("kitchen_minutes"),
                        rs.getBoolean("call_enabled"), rs.getInt("call_minutes"), rs.getBoolean("tab_enabled"),
                        rs.getInt("tab_hours")))
                .optional();
    }

    @Override
    public void saveSettings(EstablishmentId establishmentId, AlertSettings s) {
        jdbc.sql("""
                        INSERT INTO report_alert_setting (establishment_id, kitchen_enabled, kitchen_minutes, call_enabled,
                                                          call_minutes, tab_enabled, tab_hours, updated_at)
                        VALUES (:establishmentId, :kitchenEnabled, :kitchenMinutes, :callEnabled, :callMinutes,
                                :tabEnabled, :tabHours, now())
                        ON CONFLICT (establishment_id) DO UPDATE SET
                            kitchen_enabled = EXCLUDED.kitchen_enabled, kitchen_minutes = EXCLUDED.kitchen_minutes,
                            call_enabled = EXCLUDED.call_enabled, call_minutes = EXCLUDED.call_minutes,
                            tab_enabled = EXCLUDED.tab_enabled, tab_hours = EXCLUDED.tab_hours, updated_at = now()
                        """)
                .param("establishmentId", establishmentId.value())
                .param("kitchenEnabled", s.kitchenEnabled())
                .param("kitchenMinutes", s.kitchenMinutes())
                .param("callEnabled", s.callEnabled())
                .param("callMinutes", s.callMinutes())
                .param("tabEnabled", s.tabEnabled())
                .param("tabHours", s.tabHours())
                .update();
    }

    @Override
    public List<EstablishmentId> candidates() {
        return jdbc.sql("""
                        SELECT e.id FROM establishment e
                        LEFT JOIN report_alert_setting s ON s.establishment_id = e.id
                        WHERE e.active
                          AND (s.establishment_id IS NULL OR s.kitchen_enabled OR s.call_enabled OR s.tab_enabled)
                        ORDER BY e.id
                        """)
                .query((rs, row) -> new EstablishmentId(rs.getObject("id", UUID.class)))
                .list();
    }

    /** Uma linha de contagem: vazio quando nada passou do limite. */
    private static Optional<AlertFinding> finding(ResultSet rs, AlertType type, boolean withTab) throws SQLException {
        int count = rs.getInt("n");
        if (count == 0) {
            return Optional.empty();
        }
        Optional<TabId> tab = withTab
                ? Optional.ofNullable(rs.getObject("tab_id", UUID.class)).map(TabId::new)
                : Optional.empty();
        return Optional.of(new AlertFinding(type, count, rs.getString("oldest"),
                rs.getObject("newest", OffsetDateTime.class).toInstant(), tab));
    }

    @Override
    public Optional<AlertFinding> lateKitchen(EstablishmentId establishmentId, Instant before) {
        // A cozinha recebe o item no envio do pedido ou, com confirmação do garçom, na confirmação. A
        // passagem não entra: os cortes não passam pela fila da cozinha.
        return jdbc.sql("""
                        SELECT count(*) AS n,
                               (array_agg(dt.label ORDER BY greatest(i.created_at, coalesce(o.approved_at, i.created_at)),
                                          i.id))[1] AS oldest,
                               max(greatest(i.created_at, coalesce(o.approved_at, i.created_at))) AS newest
                        FROM order_item i
                        JOIN customer_order o ON o.id = i.order_id AND o.establishment_id = :establishmentId
                          AND o.status = 'SENT'
                        JOIN dining_table dt ON dt.id = o.table_id AND dt.establishment_id = :establishmentId
                        WHERE i.establishment_id = :establishmentId AND i.status IN ('PENDING', 'PREPARING')
                          AND greatest(i.created_at, coalesce(o.approved_at, i.created_at)) < :before
                          AND NOT EXISTS (SELECT 1 FROM tab_rodizio r
                                          WHERE r.id = i.tab_rodizio_id AND r.rodizio_type = 'PASSAGEM')
                        """)
                .param("establishmentId", establishmentId.value())
                .param("before", utc(before))
                .query((rs, row) -> finding(rs, AlertType.KITCHEN, false))
                .single();
    }

    @Override
    public Optional<AlertFinding> unansweredCalls(EstablishmentId establishmentId, Instant before) {
        return jdbc.sql("""
                        SELECT count(DISTINCT c.table_id) AS n,
                               (array_agg(dt.label ORDER BY c.created_at, c.id))[1] AS oldest,
                               max(c.created_at) AS newest
                        FROM call c
                        JOIN dining_table dt ON dt.id = c.table_id AND dt.establishment_id = :establishmentId
                        WHERE c.establishment_id = :establishmentId AND c.status = 'PENDING'
                          AND c.created_at < :before
                        """)
                .param("establishmentId", establishmentId.value())
                .param("before", utc(before))
                .query((rs, row) -> finding(rs, AlertType.CALL, false))
                .single();
    }

    @Override
    public Optional<AlertFinding> longTabs(EstablishmentId establishmentId, Instant before) {
        return jdbc.sql("""
                        SELECT count(*) AS n,
                               (array_agg(dt.label ORDER BY t.opened_at, t.id))[1] AS oldest,
                               (array_agg(t.id ORDER BY t.opened_at, t.id))[1] AS tab_id,
                               max(t.opened_at) AS newest
                        FROM tab t
                        JOIN dining_table dt ON dt.id = t.table_id AND dt.establishment_id = :establishmentId
                        WHERE t.establishment_id = :establishmentId AND t.status IN ('OPEN', 'CLOSING')
                          AND t.opened_at < :before
                        """)
                .param("establishmentId", establishmentId.value())
                .param("before", utc(before))
                .query((rs, row) -> finding(rs, AlertType.TAB, true))
                .single();
    }

    @Override
    public Optional<Instant> lastSent(EstablishmentId establishmentId, AlertType type) {
        return jdbc.sql("""
                        SELECT sent_at FROM report_alert_sent
                        WHERE establishment_id = :establishmentId AND alert_type = :type
                        """)
                .param("establishmentId", establishmentId.value())
                .param("type", type.name())
                .query((rs, row) -> rs.getObject("sent_at", OffsetDateTime.class).toInstant())
                .optional();
    }

    @Override
    public boolean claim(EstablishmentId establishmentId, AlertType type, Instant now, Instant notAfter) {
        return jdbc.sql("""
                        INSERT INTO report_alert_sent (establishment_id, alert_type, sent_at)
                        VALUES (:establishmentId, :type, :now)
                        ON CONFLICT (establishment_id, alert_type) DO UPDATE SET sent_at = EXCLUDED.sent_at
                        WHERE report_alert_sent.sent_at <= :notAfter
                        """)
                .param("establishmentId", establishmentId.value())
                .param("type", type.name())
                .param("now", utc(now))
                .param("notAfter", utc(notAfter))
                .update() == 1;
    }
}
