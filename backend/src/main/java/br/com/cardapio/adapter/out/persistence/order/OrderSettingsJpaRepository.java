package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface OrderSettingsJpaRepository extends JpaRepository<OrderSettingsJpaEntity, UUID> {

    Optional<OrderSettingsJpaEntity> findByEstablishmentId(UUID establishmentId);
}
