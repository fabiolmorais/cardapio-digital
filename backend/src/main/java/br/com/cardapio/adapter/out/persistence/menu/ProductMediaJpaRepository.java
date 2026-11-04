package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductMediaJpaRepository extends JpaRepository<ProductMediaJpaEntity, UUID> {

    Optional<ProductMediaJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    List<ProductMediaJpaEntity> findByEstablishmentIdAndProductIdAndActiveTrueOrderByPositionAscIdAsc(
            UUID establishmentId, UUID productId);

    /**
     * A capa de cada produto: a FOTO ativa de menor posição (com o vídeo na galeria, a capa
     * continua sendo uma foto: é o que a lista do cardápio mostra). A subconsulta acha a menor
     * posição entre as fotos do mesmo produto; a consulta de fora pega a foto que está nela. Uma
     * ida ao banco para o cardápio inteiro.
     */
    @Query("""
            SELECT m FROM ProductMediaJpaEntity m
             WHERE m.establishmentId = :establishmentId
               AND m.active = true AND m.type = 'IMAGE'
               AND m.position = (SELECT min(m2.position) FROM ProductMediaJpaEntity m2
                                  WHERE m2.productId = m.productId AND m2.active = true AND m2.type = 'IMAGE')
            """)
    List<ProductMediaJpaEntity> findCovers(@Param("establishmentId") UUID establishmentId);

    @Query("""
            SELECT coalesce(max(m.position) + 1, 0) FROM ProductMediaJpaEntity m
             WHERE m.establishmentId = :establishmentId AND m.productId = :productId
            """)
    int nextPosition(@Param("establishmentId") UUID establishmentId, @Param("productId") UUID productId);

    Optional<ProductMediaJpaEntity> findByVideoId(String videoId);
}
