package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface RodizioJpaRepository extends JpaRepository<RodizioJpaEntity, UUID> {

    Optional<RodizioJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    Page<RodizioJpaEntity> findByEstablishmentIdAndActiveTrueOrderByNameAscIdAsc(UUID establishmentId,
                                                                               Pageable pageable);

    Page<RodizioJpaEntity> findByEstablishmentIdOrderByActiveDescNameAscIdAsc(UUID establishmentId, Pageable pageable);

    long countByEstablishmentIdAndActiveTrue(UUID establishmentId);

    /** Outro rodízio ativo com o mesmo nome, sem diferenciar maiúscula (como o índice único do banco). */
    @Query("""
            SELECT count(r) > 0 FROM RodizioJpaEntity r
             WHERE r.establishmentId = :establishmentId AND r.active = true
               AND lower(r.name) = lower(:name) AND (:exceptId IS NULL OR r.id <> :exceptId)
            """)
    boolean activeNameExists(@Param("establishmentId") UUID establishmentId, @Param("name") String name,
                             @Param("exceptId") UUID exceptId);
}
