package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MenuCategoryJpaRepository extends JpaRepository<MenuCategoryJpaEntity, UUID> {

    /** Categorias ativas com a contagem de produtos ativos numa subconsulta: uma ida ao banco só. */
    @Query(value = """
            SELECT c.id AS id, c.establishmentId AS establishmentId, c.name AS name,
                   c.position AS position, c.active AS active,
                   (SELECT count(p) FROM ProductJpaEntity p
                     WHERE p.categoryId = c.id AND p.active = true) AS activeProducts
              FROM MenuCategoryJpaEntity c
             WHERE c.establishmentId = :establishmentId
               AND c.active = true
             ORDER BY c.position, c.id
            """,
            countQuery = """
            SELECT count(c) FROM MenuCategoryJpaEntity c
             WHERE c.establishmentId = :establishmentId AND c.active = true
            """)
    Page<MenuCategoryRow> findOverview(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    List<MenuCategoryJpaEntity> findByEstablishmentIdAndActiveTrueOrderByPositionAscIdAsc(UUID establishmentId);

    Optional<MenuCategoryJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    @Query("""
            SELECT count(c) > 0 FROM MenuCategoryJpaEntity c
             WHERE c.establishmentId = :establishmentId
               AND c.active = true
               AND lower(c.name) = lower(:name)
               AND c.id <> :exceptId
            """)
    boolean activeNameExists(@Param("establishmentId") UUID establishmentId, @Param("name") String name,
                             @Param("exceptId") UUID exceptId);

    @Query("SELECT coalesce(max(c.position) + 1, 0) FROM MenuCategoryJpaEntity c WHERE c.establishmentId = :establishmentId")
    int nextPosition(@Param("establishmentId") UUID establishmentId);
}
