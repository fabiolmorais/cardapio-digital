package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {

    Optional<OrderItemJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    /**
     * Os itens de vários pedidos de uma vez (a comanda inteira): uma consulta, e não uma por pedido.
     * Na ordem do carrinho (position); o adaptador separa por pedido mantendo essa ordem.
     */
    List<OrderItemJpaEntity> findByEstablishmentIdAndOrderIdInOrderByPositionAsc(UUID establishmentId,
                                                                               Collection<UUID> orderIds);

    /** Grava a mudança só se o item ainda estiver no status esperado: 1 linha ou 0. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OrderItemJpaEntity i
               SET i.status = :status, i.preparingAt = :preparingAt, i.readyAt = :readyAt,
                   i.deliveredAt = :deliveredAt, i.deliveredBy = :deliveredBy, i.canceledAt = :canceledAt,
                   i.canceledBy = :canceledBy, i.cancelReason = :cancelReason
             WHERE i.id = :id AND i.establishmentId = :establishmentId AND i.status = :expected
            """)
    int updateIfStatus(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                       @Param("expected") String expected, @Param("status") String status,
                       @Param("preparingAt") Instant preparingAt, @Param("readyAt") Instant readyAt,
                       @Param("deliveredAt") Instant deliveredAt, @Param("deliveredBy") UUID deliveredBy,
                       @Param("canceledAt") Instant canceledAt, @Param("canceledBy") UUID canceledBy,
                       @Param("cancelReason") String cancelReason);

    /** A quantidade que sobrou, só se o item ainda estiver no status e na quantidade de antes: 1 linha ou 0. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OrderItemJpaEntity i SET i.quantity = :quantity
             WHERE i.id = :id AND i.establishmentId = :establishmentId AND i.status = :expected
               AND i.quantity = :expectedQuantity
            """)
    int updateQuantityIfUnchanged(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                                  @Param("expected") String expected,
                                  @Param("expectedQuantity") int expectedQuantity,
                                  @Param("quantity") int quantity);

    /** Cancela de uma vez os itens ainda a caminho de um pedido (o pedido recusado pelo garçom). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OrderItemJpaEntity i
               SET i.status = 'CANCELED', i.canceledAt = :now, i.canceledBy = :by, i.cancelReason = :reason
             WHERE i.orderId = :orderId AND i.establishmentId = :establishmentId
               AND i.status IN ('PENDING', 'PREPARING', 'READY')
            """)
    int cancelOnTheWay(@Param("orderId") UUID orderId, @Param("establishmentId") UUID establishmentId,
                       @Param("by") UUID by, @Param("reason") String reason, @Param("now") Instant now);

    /**
     * A fila da cozinha: o item com o número do pedido, a mesa e o setor, numa consulta só. Só os
     * pedidos que foram para a cozinha (SENT); por ordem de chegada e, no mesmo pedido, na do carrinho.
     */
    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.KitchenItemRow(
                       i, o.number, o.tableId, t.label, s.name)
              FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
              JOIN DiningTableJpaEntity t ON t.id = o.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT' AND i.status IN :statuses
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
             ORDER BY i.createdAt, o.number, i.position
            """,
            countQuery = """
            SELECT count(i) FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT' AND i.status IN :statuses
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
            """)
    Page<KitchenItemRow> findQueue(@Param("establishmentId") UUID establishmentId,
                                   @Param("statuses") Collection<String> statuses, Pageable pageable);

    /** Os cortes do rodízio de passagem a caminho (na fila, em preparo ou prontos), dos pedidos enviados. */
    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.KitchenItemRow(
                       i, o.number, o.tableId, t.label, s.name)
              FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
              JOIN TabRodizioJpaEntity r ON r.id = i.tabRodizioId
              JOIN DiningTableJpaEntity t ON t.id = o.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.status IN ('PENDING', 'PREPARING', 'READY') AND r.rodizioType = 'PASSAGEM'
             ORDER BY i.createdAt, o.number, i.position
            """,
            countQuery = """
            SELECT count(i) FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
              JOIN TabRodizioJpaEntity r ON r.id = i.tabRodizioId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.status IN ('PENDING', 'PREPARING', 'READY') AND r.rodizioType = 'PASSAGEM'
            """)
    Page<KitchenItemRow> findPassagemQueue(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    /** Os produtos mais pedidos desde `since` (quantidade somada), sem cancelados nem rodízio. */
    @Query(value = """
            SELECT i.product_id FROM order_item i
             WHERE i.establishment_id = :establishmentId AND i.created_at >= :since
               AND i.status <> 'CANCELED' AND i.tab_rodizio_id IS NULL
             GROUP BY i.product_id
             ORDER BY sum(i.quantity) DESC, i.product_id
             LIMIT :max
            """, nativeQuery = true)
    List<UUID> findMostOrdered(@Param("establishmentId") UUID establishmentId,
                                                   @Param("since") Instant since,
                                                   @Param("max") int max);
}
