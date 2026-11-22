package br.com.cardapio.adapter.out.persistence.order;

import br.com.cardapio.application.port.out.order.DuplicateWarningPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.TabId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.UUID;

/** Os avisos de pedido repetido, em SQL parametrizado. */
@Component
class DuplicateWarningPersistenceAdapter implements DuplicateWarningPort {

    private final JdbcClient jdbc;

    DuplicateWarningPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Numa transação nova: o envio que gerou o aviso é recusado e desfeito, e o aviso fica. A tabela
     * não tem chave estrangeira para a comanda, para esta transação não esperar a trava que o envio
     * segura nela.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void warn(EstablishmentId establishmentId, TabId tabId, UserId warnedBy, Collection<ProductId> products,
                     Instant now) {
        for (ProductId product : products) {
            jdbc.sql("""
                            INSERT INTO duplicate_warning (id, establishment_id, tab_id, product_id, warned_by, warned_at)
                            SELECT :id, :establishmentId, :tabId, :productId, :warnedBy, :now
                            WHERE NOT EXISTS (
                                SELECT 1 FROM duplicate_warning w
                                WHERE w.establishment_id = :establishmentId AND w.tab_id = :tabId
                                  AND w.product_id = :productId AND w.warned_by = :warnedBy
                                  AND w.confirmed_at IS NULL)
                            """)
                    .param("id", UUID.randomUUID())
                    .param("establishmentId", establishmentId.value())
                    .param("tabId", tabId.value())
                    .param("productId", product.value())
                    .param("warnedBy", warnedBy.value())
                    .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                    .update();
        }
    }

    @Override
    public void confirm(EstablishmentId establishmentId, TabId tabId, UserId warnedBy, Collection<ProductId> products,
                        Instant now) {
        OffsetDateTime at = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        for (ProductId product : products) {
            int confirmed = jdbc.sql("""
                            UPDATE duplicate_warning SET confirmed_at = :now
                            WHERE establishment_id = :establishmentId AND tab_id = :tabId AND product_id = :productId
                              AND warned_by = :warnedBy AND confirmed_at IS NULL
                            """)
                    .param("establishmentId", establishmentId.value())
                    .param("tabId", tabId.value())
                    .param("productId", product.value())
                    .param("warnedBy", warnedBy.value())
                    .param("now", at)
                    .update();
            if (confirmed == 0) {
                // Confirmou sem um aviso gravado antes (por exemplo, o aviso veio de outro garçom).
                jdbc.sql("""
                                INSERT INTO duplicate_warning (id, establishment_id, tab_id, product_id, warned_by,
                                                               warned_at, confirmed_at)
                                VALUES (:id, :establishmentId, :tabId, :productId, :warnedBy, :now, :now)
                                """)
                        .param("id", UUID.randomUUID())
                        .param("establishmentId", establishmentId.value())
                        .param("tabId", tabId.value())
                        .param("productId", product.value())
                        .param("warnedBy", warnedBy.value())
                        .param("now", at)
                        .update();
            }
        }
    }
}
