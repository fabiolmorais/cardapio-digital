package br.com.cardapio.adapter.out.persistence.menu;

import br.com.cardapio.application.port.out.menu.ProductStatusHistoryPort;
import br.com.cardapio.domain.menu.model.ProductStatusChange;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** O histórico de promoção, destaque e "disponível" dos produtos, em SQL parametrizado. */
@Component
class ProductStatusHistoryPersistenceAdapter implements ProductStatusHistoryPort {

    private final JdbcClient jdbc;
    private final EntityManager entityManager;

    ProductStatusHistoryPersistenceAdapter(JdbcClient jdbc, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Override
    public void record(List<ProductStatusChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        // O produto novo pode estar só no contexto do JPA: vai para o banco antes (a chave estrangeira).
        entityManager.flush();
        for (ProductStatusChange change : changes) {
            jdbc.sql("""
                            INSERT INTO product_status_change (id, establishment_id, product_id, kind, active,
                                                               promo_price_cents, changed_at)
                            VALUES (:id, :establishmentId, :productId, :kind, :active, :promo, :at)
                            """)
                    .param("id", UUID.randomUUID())
                    .param("establishmentId", change.establishmentId().value())
                    .param("productId", change.productId().value())
                    .param("kind", change.kind().name())
                    .param("active", change.active())
                    .param("promo", change.promoPrice() == null ? null : change.promoPrice().cents())
                    .param("at", OffsetDateTime.ofInstant(change.at(), ZoneOffset.UTC))
                    .update();
        }
    }
}
