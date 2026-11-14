package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OptionItemJpaRepository extends JpaRepository<OptionItemJpaEntity, UUID> {

    Optional<OptionItemJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    List<OptionItemJpaEntity> findByEstablishmentIdAndOptionGroupIdInAndActiveTrueOrderByPositionAsc(
            UUID establishmentId, Collection<UUID> groupIds);
}
