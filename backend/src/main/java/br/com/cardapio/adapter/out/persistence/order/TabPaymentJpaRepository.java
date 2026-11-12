package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TabPaymentJpaRepository extends JpaRepository<TabPaymentJpaEntity, UUID> {

    List<TabPaymentJpaEntity> findByEstablishmentIdAndTabIdOrderByReceivedAtAsc(UUID establishmentId, UUID tabId);
}
