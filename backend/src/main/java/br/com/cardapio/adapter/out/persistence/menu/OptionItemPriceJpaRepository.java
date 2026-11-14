package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface OptionItemPriceJpaRepository extends JpaRepository<OptionItemPriceJpaEntity, UUID> {

    List<OptionItemPriceJpaEntity> findByEstablishmentIdAndProductIdIn(UUID establishmentId,
                                                                       Collection<UUID> productIds);

    List<OptionItemPriceJpaEntity> findByEstablishmentId(UUID establishmentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM OptionItemPriceJpaEntity p WHERE p.establishmentId = :establishmentId AND p.productId = :productId")
    void deleteByProduct(@Param("establishmentId") UUID establishmentId, @Param("productId") UUID productId);
}
