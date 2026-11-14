package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface OrderItemOptionJpaRepository extends JpaRepository<OrderItemOptionJpaEntity, UUID> {

    List<OrderItemOptionJpaEntity> findByEstablishmentIdAndOrderItemIdInOrderByPositionAsc(UUID establishmentId,
                                                                                          Collection<UUID> orderItemIds);
}
