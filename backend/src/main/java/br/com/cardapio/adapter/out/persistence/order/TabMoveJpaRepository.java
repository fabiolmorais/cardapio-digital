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

/**
 * As mudanças de mesa (ADR 0012). As consultas nativas não passam pelo filtro do Hibernate: todas
 * têm o establishment_id no WHERE.
 */
interface TabMoveJpaRepository extends JpaRepository<TabMoveJpaEntity, UUID> {

    /** Os itens a caminho da comanda que já saíram na impressora: a via leva estes. */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO tab_move_item (tab_move_id, order_item_id, establishment_id)
            SELECT :moveId, i.id, i.establishment_id
              FROM order_item i
              JOIN customer_order o ON o.id = i.order_id
             WHERE o.tab_id = :tabId AND o.establishment_id = :establishmentId
               AND i.printed_at IS NOT NULL AND i.status IN ('PENDING', 'PREPARING', 'READY')
            """)
    int recordPrintedOnTheWay(@Param("establishmentId") UUID establishmentId, @Param("moveId") UUID moveId,
                              @Param("tabId") UUID tabId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE tab_move SET needs_print = TRUE WHERE id = :moveId AND establishment_id = :establishmentId
            """)
    int markNeedsPrint(@Param("establishmentId") UUID establishmentId, @Param("moveId") UUID moveId);

    @Query(nativeQuery = true, value = """
            SELECT f.label AS fromTableLabel, t.label AS toTableLabel, (m.merged_tab_id IS NOT NULL) AS merged,
                   u.name AS movedByName, m.moved_at AS movedAt
              FROM tab_move m
              JOIN dining_table f ON f.id = m.from_table_id
              JOIN dining_table t ON t.id = m.to_table_id
              LEFT JOIN app_user u ON u.id = m.moved_by
             WHERE m.establishment_id = :establishmentId AND m.tab_id = :tabId
             ORDER BY m.moved_at, m.id
            """)
    List<TabMoveRows.History> findByTab(@Param("establishmentId") UUID establishmentId, @Param("tabId") UUID tabId);

    @Query(nativeQuery = true, value = """
            SELECT merged_into_tab_id FROM tab
             WHERE id = :tabId AND establishment_id = :establishmentId AND merged_into_tab_id IS NOT NULL
            """)
    Optional<UUID> findMergedInto(@Param("establishmentId") UUID establishmentId, @Param("tabId") UUID tabId);

    @Query(nativeQuery = true, value = """
            SELECT EXISTS (SELECT 1 FROM tab_move
                            WHERE establishment_id = :establishmentId AND from_table_id = :fromTableId
                              AND to_table_id = :toTableId AND moved_at >= :since)
            """)
    boolean movedBetween(@Param("establishmentId") UUID establishmentId, @Param("fromTableId") UUID from,
                         @Param("toTableId") UUID to, @Param("since") Instant since);

    // ---- A via impressa

    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.order.MovePrintRow(
                       m.id, m.mergedTabId, f.label, t.label, s.name, m.movedAt, m.needsPrint, m.printedAt)
              FROM TabMoveJpaEntity m
              JOIN DiningTableJpaEntity f ON f.id = m.fromTableId
              JOIN DiningTableJpaEntity t ON t.id = m.toTableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE m.establishmentId = :establishmentId AND m.needsPrint = TRUE AND m.printedAt IS NULL
             ORDER BY m.movedAt, m.id
            """,
            countQuery = """
            SELECT count(m) FROM TabMoveJpaEntity m
             WHERE m.establishmentId = :establishmentId AND m.needsPrint = TRUE AND m.printedAt IS NULL
            """)
    Page<MovePrintRow> findPendingPrints(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    @Query("""
            SELECT new br.com.cardapio.adapter.out.persistence.order.MovePrintRow(
                       m.id, m.mergedTabId, f.label, t.label, s.name, m.movedAt, m.needsPrint, m.printedAt)
              FROM TabMoveJpaEntity m
              JOIN DiningTableJpaEntity f ON f.id = m.fromTableId
              JOIN DiningTableJpaEntity t ON t.id = m.toTableId
              JOIN SectorJpaEntity s ON s.id = t.sectorId
             WHERE m.id = :moveId AND m.establishmentId = :establishmentId
            """)
    Optional<MovePrintRow> findPrintHeader(@Param("establishmentId") UUID establishmentId,
                                           @Param("moveId") UUID moveId);

    /** Trava a mudança até o fim da transação: duas estações pegando a mesma via, uma de cada vez. */
    @Query(nativeQuery = true, value = """
            SELECT id FROM tab_move WHERE id = :moveId AND establishment_id = :establishmentId FOR UPDATE
            """)
    Optional<UUID> lock(@Param("establishmentId") UUID establishmentId, @Param("moveId") UUID moveId);

    /** Os itens das vias que ainda estão a caminho (os entregues e cancelados saem da via). */
    @Query(nativeQuery = true, value = """
            SELECT mi.tab_move_id AS moveId, mi.order_item_id AS itemId
              FROM tab_move_item mi
              JOIN order_item i ON i.id = mi.order_item_id
             WHERE mi.establishment_id = :establishmentId AND mi.tab_move_id IN (:moveIds)
               AND i.status IN ('PENDING', 'PREPARING', 'READY')
            """)
    List<TabMoveRows.MoveItem> findPrintItems(@Param("establishmentId") UUID establishmentId,
                                              @Param("moveIds") Collection<UUID> moveIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            UPDATE tab_move SET printed_at = :now
             WHERE id = :moveId AND establishment_id = :establishmentId AND needs_print AND printed_at IS NULL
            """)
    int markPrinted(@Param("establishmentId") UUID establishmentId, @Param("moveId") UUID moveId,
                    @Param("now") Instant now);
}
