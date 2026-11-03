package br.com.cardapio.adapter.out.persistence.order;

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
}
