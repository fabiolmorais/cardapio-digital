package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductVariantJpaRepository extends JpaRepository<ProductVariantJpaEntity, UUID> {

    Optional<ProductVariantJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    List<ProductVariantJpaEntity> findByEstablishmentIdAndProductIdOrderByPositionAsc(UUID establishmentId,
                                                                                      UUID productId);

    List<ProductVariantJpaEntity> findByEstablishmentIdAndProductIdInAndActiveTrueOrderByPositionAsc(
            UUID establishmentId, Collection<UUID> productIds);

    List<ProductVariantJpaEntity> findByEstablishmentIdAndActiveTrueOrderByPositionAsc(UUID establishmentId);
}
