package br.com.cardapio.adapter.out.persistence.presence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PresenceRuleJpaRepository extends JpaRepository<PresenceRuleJpaEntity, UUID> {

    Optional<PresenceRuleJpaEntity> findByEstablishmentId(UUID establishmentId);
}
