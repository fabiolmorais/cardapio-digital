package br.com.cardapio.adapter.out.persistence.menu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    /**
     * A busca: produtos ativos cujo nome contém o padrão. O "ESCAPE '!'" diz ao banco que "!%"
     * é um % de verdade, e não o curinga (o adaptador prepara o padrão assim).
     */
    String SEARCH = """
            FROM ProductJpaEntity p JOIN MenuCategoryJpaEntity c ON c.id = p.categoryId
             WHERE p.establishmentId = :establishmentId
               AND p.active = true
               AND lower(p.name) LIKE :pattern ESCAPE '!'
            """;
    /** A ordem do cardápio: primeiro a das categorias, depois a de dentro de cada uma. */
    String ORDER = " ORDER BY c.position, c.id, p.position, p.id";

    @Query(value = "SELECT p " + SEARCH + ORDER, countQuery = "SELECT count(p) " + SEARCH)
    Page<ProductJpaEntity> search(@Param("establishmentId") UUID establishmentId, @Param("pattern") String pattern,
                                  Pageable pageable);

    @Query(value = "SELECT p " + SEARCH + " AND p.categoryId = :categoryId" + ORDER,
            countQuery = "SELECT count(p) " + SEARCH + " AND p.categoryId = :categoryId")
    Page<ProductJpaEntity> searchInCategory(@Param("establishmentId") UUID establishmentId,
                                            @Param("categoryId") UUID categoryId, @Param("pattern") String pattern,
                                            Pageable pageable);

    Optional<ProductJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    List<ProductJpaEntity> findByEstablishmentIdAndCategoryIdAndActiveTrueOrderByPositionAscIdAsc(
            UUID establishmentId, UUID categoryId);

    @Query("""
            SELECT coalesce(max(p.position) + 1, 0) FROM ProductJpaEntity p
             WHERE p.establishmentId = :establishmentId AND p.categoryId = :categoryId
            """)
    int nextPosition(@Param("establishmentId") UUID establishmentId, @Param("categoryId") UUID categoryId);

    @Query("""
            SELECT count(p) FROM ProductJpaEntity p
             WHERE p.establishmentId = :establishmentId AND p.categoryId = :categoryId AND p.active = true
            """)
    long countActiveByCategory(@Param("establishmentId") UUID establishmentId, @Param("categoryId") UUID categoryId);
}
