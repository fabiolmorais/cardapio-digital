package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.application.port.out.menu.MenuEventPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.MenuEvent;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Os eventos do cardápio em SQL parametrizado. Cada evento só entra se o prato for deste restaurante
 * (o INSERT parte do produto): um celular não grava evento no cardápio de outro.
 */
@Component
class MenuEventPersistenceAdapter implements MenuEventPort {

    private final JdbcClient jdbc;

    MenuEventPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int countForSession(TableSessionId sessionId) {
        return jdbc.sql("SELECT count(*) FROM menu_event WHERE session_id = :session")
                .param("session", sessionId.value())
                .query(Integer.class)
                .single();
    }

    @Override
    public int save(EstablishmentId establishmentId, TableSessionId sessionId, List<MenuEvent> events, Instant at) {
        int saved = 0;
        OffsetDateTime when = OffsetDateTime.ofInstant(at, ZoneOffset.UTC);
        for (MenuEvent event : events) {
            saved += jdbc.sql("""
                            INSERT INTO menu_event (id, establishment_id, session_id, product_id, kind, quantity,
                                                    occurred_at)
                            SELECT :id, p.establishment_id, :session, p.id, :kind, :quantity, :at
                            FROM product p
                            WHERE p.id = :product AND p.establishment_id = :establishmentId
                            """)
                    .param("id", UUID.randomUUID())
                    .param("session", sessionId.value())
                    .param("kind", event.kind().name())
                    .param("quantity", event.quantity())
                    .param("at", when)
                    .param("product", event.productId().value())
                    .param("establishmentId", establishmentId.value())
                    .update();
        }
        return saved;
    }

    @Override
    public int deleteBefore(Instant before) {
        return jdbc.sql("DELETE FROM menu_event WHERE occurred_at < :before")
                .param("before", OffsetDateTime.ofInstant(before, ZoneOffset.UTC))
                .update();
    }
}
