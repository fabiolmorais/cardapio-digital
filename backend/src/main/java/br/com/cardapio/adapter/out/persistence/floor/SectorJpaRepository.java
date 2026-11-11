package br.com.cardapio.adapter.out.persistence.floor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SectorJpaRepository extends JpaRepository<SectorJpaEntity, UUID> {

    /**
     * Setores (os ativos e, com includeInactive, os desativados depois) com a contagem de mesas
     * ativas numa subconsulta: uma ida ao banco só (sem N+1).
     */
    @Query(value = """
            SELECT s.id AS id, s.establishmentId AS establishmentId, s.name AS name,
                   s.position AS position, s.active AS active,
                   (SELECT count(t) FROM DiningTableJpaEntity t
                     WHERE t.sectorId = s.id AND t.active = true) AS activeTables
              FROM SectorJpaEntity s
             WHERE s.establishmentId = :establishmentId
               AND (s.active = true OR :includeInactive = true)
             ORDER BY s.active DESC, s.position, s.id
            """,
            countQuery = """
            SELECT count(s) FROM SectorJpaEntity s
             WHERE s.establishmentId = :establishmentId AND (s.active = true OR :includeInactive = true)
            """)
    Page<SectorRow> findAll(@Param("establishmentId") UUID establishmentId,
                            @Param("includeInactive") boolean includeInactive, Pageable pageable);

    Optional<SectorJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    /** exceptId nunca é nulo: quem chama passa um UUID que não existe quando não há o que excluir. */
    @Query("""
            SELECT count(s) > 0 FROM SectorJpaEntity s
             WHERE s.establishmentId = :establishmentId
               AND s.active = true
               AND lower(s.name) = lower(:name)
               AND s.id <> :exceptId
            """)
    boolean activeNameExists(@Param("establishmentId") UUID establishmentId, @Param("name") String name,
                             @Param("exceptId") UUID exceptId);

    @Query("SELECT coalesce(max(s.position) + 1, 0) FROM SectorJpaEntity s WHERE s.establishmentId = :establishmentId")
    int nextPosition(@Param("establishmentId") UUID establishmentId);
}
