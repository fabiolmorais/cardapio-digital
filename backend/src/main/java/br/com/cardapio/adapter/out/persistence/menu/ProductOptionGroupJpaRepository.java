package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface ProductOptionGroupJpaRepository extends JpaRepository<ProductOptionGroupJpaEntity, UUID> {

    List<ProductOptionGroupJpaEntity> findByEstablishmentIdAndProductIdInOrderByPositionAsc(UUID establishmentId,
                                                                                          Collection<UUID> productIds);

    List<ProductOptionGroupJpaEntity> findByEstablishmentIdOrderByPositionAsc(UUID establishmentId);

    /** Em quantos produtos ativos cada grupo está ligado: [id do grupo, quantos]. */
    @Query("""
            SELECT l.optionGroupId, count(l) FROM ProductOptionGroupJpaEntity l, ProductJpaEntity p
             WHERE p.id = l.productId AND p.active = true
               AND l.establishmentId = :establishmentId AND l.optionGroupId IN :groupIds
             GROUP BY l.optionGroupId
            """)
    List<Object[]> countActiveProducts(@Param("establishmentId") UUID establishmentId,
                                       @Param("groupIds") Collection<UUID> groupIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ProductOptionGroupJpaEntity l WHERE l.establishmentId = :establishmentId AND l.productId = :productId")
    void deleteByProduct(@Param("establishmentId") UUID establishmentId, @Param("productId") UUID productId);
}
