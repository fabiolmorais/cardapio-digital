package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TabJpaRepository extends JpaRepository<TabJpaEntity, UUID> {

    @Query("""
            SELECT t FROM TabJpaEntity t
             WHERE t.establishmentId = :establishmentId AND t.tableId = :tableId AND t.status IN ('OPEN', 'CLOSING')
            """)
    Optional<TabJpaEntity> findOpen(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId);

    /** Abre a comanda, a menos que o índice "uma aberta por mesa" já tenha uma (aí não grava: 0 linhas). */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO tab (id, establishment_id, table_id, status, opened_at)
            VALUES (:id, :establishmentId, :tableId, 'OPEN', :openedAt)
            ON CONFLICT (table_id) WHERE status IN ('OPEN', 'CLOSING') DO NOTHING
            """)
    int insertIfNoneOpen(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                         @Param("tableId") UUID tableId, @Param("openedAt") Instant openedAt);

    @Query("""
            SELECT new br.com.cardapio.adapter.out.persistence.order.TabRow(t, d.label, s.name)
              FROM TabJpaEntity t
              JOIN DiningTableJpaEntity d ON d.id = t.tableId
              JOIN SectorJpaEntity s ON s.id = d.sectorId
             WHERE t.id = :id AND t.establishmentId = :establishmentId
            """)
    Optional<TabRow> findView(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId);

    /** Fecha só se ainda estiver aberta: 1 linha ou 0. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TabJpaEntity t
               SET t.status = 'CLOSED', t.closedAt = :closedAt, t.closedBy = :closedBy,
                   t.totalCents = :totalCents, t.paymentMethod = :paymentMethod
             WHERE t.id = :id AND t.establishmentId = :establishmentId AND t.status IN ('OPEN', 'CLOSING')
            """)
    int close(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
              @Param("closedAt") Instant closedAt, @Param("closedBy") UUID closedBy,
              @Param("totalCents") long totalCents, @Param("paymentMethod") String paymentMethod);

    /**
     * As comandas abertas com o resumo, numa consulta para a página: os pedidos e os itens entram
     * por LEFT JOIN (uma comanda sempre tem pedido, mas o LEFT não esconde nenhuma por engano) e o
     * banco soma por comanda (GROUP BY).
     */
    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.OpenTabRow(t, d.label, s.name,
                       COALESCE(SUM(CASE WHEN i.status <> 'CANCELED' THEN i.unitPriceCents * i.quantity
                                         ELSE 0 END), 0),
                       COALESCE(SUM(CASE WHEN i.status IN ('PENDING', 'PREPARING', 'READY') THEN 1
                                         ELSE 0 END), 0))
              FROM TabJpaEntity t
              JOIN DiningTableJpaEntity d ON d.id = t.tableId
              JOIN SectorJpaEntity s ON s.id = d.sectorId
              LEFT JOIN OrderJpaEntity o ON o.tabId = t.id
              LEFT JOIN OrderItemJpaEntity i ON i.orderId = o.id
             WHERE t.establishmentId = :establishmentId AND t.status IN ('OPEN', 'CLOSING')
             GROUP BY t, d.label, s.name
             ORDER BY t.openedAt, t.id
            """,
            countQuery = """
            SELECT count(t) FROM TabJpaEntity t
             WHERE t.establishmentId = :establishmentId AND t.status IN ('OPEN', 'CLOSING')
            """)
    Page<OpenTabRow> findOpenTabs(@Param("establishmentId") UUID establishmentId, Pageable pageable);
}
