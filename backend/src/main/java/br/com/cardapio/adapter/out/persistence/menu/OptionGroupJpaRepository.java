package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OptionGroupJpaRepository extends JpaRepository<OptionGroupJpaEntity, UUID> {

    Optional<OptionGroupJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    Page<OptionGroupJpaEntity> findByEstablishmentIdAndActiveTrueOrderByNameAscIdAsc(UUID establishmentId,
                                                                                    Pageable page);

    List<OptionGroupJpaEntity> findByEstablishmentIdAndActiveTrueAndIdIn(UUID establishmentId, Collection<UUID> ids);

    long countByEstablishmentIdAndActiveTrue(UUID establishmentId);
}
