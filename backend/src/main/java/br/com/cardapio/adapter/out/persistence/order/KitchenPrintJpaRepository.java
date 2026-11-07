package br.com.cardapio.adapter.out.persistence.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * As consultas da comanda impressa. As mesmas regras da fila da cozinha: só pedidos que foram para
 * a cozinha (SENT) e sem os cortes do rodízio de passagem (esses vão para o passador).
 * Cada lista é paginada por PEDIDO; os itens das comandas da página vêm numa segunda consulta (IN).
 */
interface KitchenPrintJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {

    String NOT_PASSAGEM = """
            NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                         WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
            """;
    /** A comanda do pedido: os itens a caminho ainda sem comanda. */
    String PENDING_ORDER = "i.printedAt IS NULL AND i.status IN ('PENDING', 'PREPARING', 'READY')";
    /** A via "CANCELADO": os itens que já tinham saído na impressora e foram cancelados depois. */
    String PENDING_CANCEL = "i.printedAt IS NOT NULL AND i.cancelPrintedAt IS NULL AND i.status = 'CANCELED'";

    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.PrintJobRow(
                       o.id, o.number, o.tableId, t.label, s.name, min(i.createdAt))
              FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
              JOIN DiningTableJpaEntity t ON t.id = o.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.printedAt IS NULL AND i.status IN ('PENDING', 'PREPARING', 'READY')
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
             GROUP BY o.id, o.number, o.tableId, t.label, s.name
             ORDER BY min(i.createdAt)
            """,
            countQuery = """
            SELECT count(DISTINCT i.orderId) FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.printedAt IS NULL AND i.status IN ('PENDING', 'PREPARING', 'READY')
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
            """)
    Page<PrintJobRow> findPendingOrders(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.PrintJobRow(
                       o.id, o.number, o.tableId, t.label, s.name, min(i.canceledAt))
              FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
              JOIN DiningTableJpaEntity t ON t.id = o.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.printedAt IS NOT NULL AND i.cancelPrintedAt IS NULL AND i.status = 'CANCELED'
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
             GROUP BY o.id, o.number, o.tableId, t.label, s.name
             ORDER BY min(i.canceledAt)
            """,
            countQuery = """
            SELECT count(DISTINCT i.orderId) FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.printedAt IS NOT NULL AND i.cancelPrintedAt IS NULL AND i.status = 'CANCELED'
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
            """)
    Page<PrintJobRow> findPendingCancellations(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.PrintJobRow(
                       o.id, o.number, o.tableId, t.label, s.name, max(i.printedAt))
              FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
              JOIN DiningTableJpaEntity t ON t.id = o.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.printedAt >= :since
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
             GROUP BY o.id, o.number, o.tableId, t.label, s.name
             ORDER BY max(i.printedAt) DESC
            """,
            countQuery = """
            SELECT count(DISTINCT i.orderId) FROM OrderItemJpaEntity i
              JOIN OrderJpaEntity o ON o.id = i.orderId
             WHERE i.establishmentId = :establishmentId AND o.status = 'SENT'
               AND i.printedAt >= :since
               AND NOT EXISTS (SELECT r.id FROM TabRodizioJpaEntity r
                                WHERE r.id = i.tabRodizioId AND r.rodizioType = 'PASSAGEM')
            """)
    Page<PrintJobRow> findPrintedSince(@Param("establishmentId") UUID establishmentId,
                                       @Param("since") Instant since, Pageable pageable);

    @Query("SELECT i FROM OrderItemJpaEntity i WHERE i.establishmentId = :establishmentId AND i.orderId IN :orderIds"
            + " AND " + PENDING_ORDER + " ORDER BY i.position")
    List<OrderItemJpaEntity> findPendingOrderItems(@Param("establishmentId") UUID establishmentId,
                                                   @Param("orderIds") Collection<UUID> orderIds);

    @Query("SELECT i FROM OrderItemJpaEntity i WHERE i.establishmentId = :establishmentId AND i.orderId IN :orderIds"
            + " AND " + PENDING_CANCEL + " ORDER BY i.position")
    List<OrderItemJpaEntity> findPendingCancelItems(@Param("establishmentId") UUID establishmentId,
                                                    @Param("orderIds") Collection<UUID> orderIds);

    /** Os itens da comanda impressa: todos os que saíram na impressora, como estão agora. */
    @Query("SELECT i FROM OrderItemJpaEntity i WHERE i.establishmentId = :establishmentId AND i.orderId IN :orderIds"
            + " AND i.printedAt IS NOT NULL ORDER BY i.position")
    List<OrderItemJpaEntity> findPrintedItems(@Param("establishmentId") UUID establishmentId,
                                              @Param("orderIds") Collection<UUID> orderIds);

    /** O cabeçalho de um pedido deste restaurante, tenha ou não algo a imprimir. */
    @Query("""
            SELECT new br.com.cardapio.adapter.out.persistence.order.PrintJobRow(
                       o.id, o.number, o.tableId, t.label, s.name, o.createdAt)
              FROM OrderJpaEntity o
              JOIN DiningTableJpaEntity t ON t.id = o.tableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE o.id = :orderId AND o.establishmentId = :establishmentId
            """)
    Optional<PrintJobRow> findHeader(@Param("establishmentId") UUID establishmentId, @Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OrderItemJpaEntity i WHERE i.establishmentId = :establishmentId AND i.orderId = :orderId"
            + " AND EXISTS (SELECT o.id FROM OrderJpaEntity o WHERE o.id = i.orderId AND o.status = 'SENT')"
            + " AND " + PENDING_ORDER + " AND " + NOT_PASSAGEM + " ORDER BY i.position")
    List<OrderItemJpaEntity> lockPendingOrderItems(@Param("establishmentId") UUID establishmentId,
                                                   @Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OrderItemJpaEntity i WHERE i.establishmentId = :establishmentId AND i.orderId = :orderId"
            + " AND " + PENDING_CANCEL + " AND " + NOT_PASSAGEM + " ORDER BY i.position")
    List<OrderItemJpaEntity> lockPendingCancelItems(@Param("establishmentId") UUID establishmentId,
                                                    @Param("orderId") UUID orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE OrderItemJpaEntity i SET i.printedAt = :now"
            + " WHERE i.id IN :ids AND i.establishmentId = :establishmentId AND i.printedAt IS NULL")
    int markOrderPrinted(@Param("establishmentId") UUID establishmentId, @Param("ids") Collection<UUID> ids,
                         @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE OrderItemJpaEntity i SET i.cancelPrintedAt = :now"
            + " WHERE i.id IN :ids AND i.establishmentId = :establishmentId AND i.cancelPrintedAt IS NULL")
    int markCancelPrinted(@Param("establishmentId") UUID establishmentId, @Param("ids") Collection<UUID> ids,
                          @Param("now") Instant now);
}
