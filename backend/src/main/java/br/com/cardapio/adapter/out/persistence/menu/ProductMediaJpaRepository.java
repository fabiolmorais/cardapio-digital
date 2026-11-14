package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductMediaJpaRepository extends JpaRepository<ProductMediaJpaEntity, UUID> {

    Optional<ProductMediaJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    List<ProductMediaJpaEntity> findByEstablishmentIdAndProductIdAndActiveTrueOrderByPositionAscIdAsc(
            UUID establishmentId, UUID productId);

    List<ProductMediaJpaEntity> findByEstablishmentIdAndOptionItemIdAndActiveTrueOrderByPositionAscIdAsc(
            UUID establishmentId, UUID optionItemId);

    /** As galerias prontas dos produtos do restaurante, por produto e na ordem de cada galeria. */
    List<ProductMediaJpaEntity>
    findByEstablishmentIdAndProductIdIsNotNullAndActiveTrueAndStatusOrderByProductIdAscPositionAscIdAsc(
            UUID establishmentId, String status);

    /** As galerias prontas das opções do restaurante, por opção e na ordem de cada galeria. */
    List<ProductMediaJpaEntity>
    findByEstablishmentIdAndOptionItemIdIsNotNullAndActiveTrueAndStatusOrderByOptionItemIdAscPositionAscIdAsc(
            UUID establishmentId, String status);

    /** Os ids de produto e de opção são UUIDs aleatórios: o mesmo id nunca é dos dois. */
    @Query("""
            SELECT coalesce(max(m.position) + 1, 0) FROM ProductMediaJpaEntity m
             WHERE m.establishmentId = :establishmentId AND (m.productId = :ownerId OR m.optionItemId = :ownerId)
            """)
    int nextPosition(@Param("establishmentId") UUID establishmentId, @Param("ownerId") UUID ownerId);

    Optional<ProductMediaJpaEntity> findByVideoId(String videoId);

    /** Fotos e vídeos de cada produto, numa consulta só para a página inteira da lista. */
    @Query("""
            SELECT m.productId as productId, m.type as type, count(m) as total FROM ProductMediaJpaEntity m
             WHERE m.establishmentId = :establishmentId AND m.productId IN :productIds
               AND m.active = true AND m.status <> 'FAILED'
             GROUP BY m.productId, m.type
            """)
    List<MediaCountRow> countByProducts(@Param("establishmentId") UUID establishmentId,
                                        @Param("productIds") Collection<UUID> productIds);
}
