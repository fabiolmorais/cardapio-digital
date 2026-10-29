package br.com.cardapio.adapter.out.persistence.floor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface DiningTableJpaRepository extends JpaRepository<DiningTableJpaEntity, UUID> {

    /**
     * Ativas primeiro; depois pelo tamanho do rótulo e só então pelo texto. Assim "2" vem antes
     * de "10" (pela ordem só de texto, "10" viria antes de "2").
     */
    String ORDER = " ORDER BY t.active DESC, length(t.label), lower(t.label), t.id";

    @Query(value = "SELECT t FROM DiningTableJpaEntity t WHERE t.establishmentId = :establishmentId" + ORDER,
            countQuery = "SELECT count(t) FROM DiningTableJpaEntity t WHERE t.establishmentId = :establishmentId")
    Page<DiningTableJpaEntity> findAll(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    @Query(value = """
            SELECT t FROM DiningTableJpaEntity t
             WHERE t.establishmentId = :establishmentId AND t.sectorId = :sectorId""" + ORDER,
            countQuery = """
            SELECT count(t) FROM DiningTableJpaEntity t
             WHERE t.establishmentId = :establishmentId AND t.sectorId = :sectorId""")
    Page<DiningTableJpaEntity> findBySector(@Param("establishmentId") UUID establishmentId,
                                            @Param("sectorId") UUID sectorId, Pageable pageable);

    Optional<DiningTableJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    Optional<DiningTableJpaEntity> findByPublicTokenAndActiveTrue(String publicToken);

    /** Nome do restaurante só se ele estiver ativo: restaurante desativado não abre página de mesa. */
    @Query("SELECT e.name FROM EstablishmentJpaEntity e WHERE e.id = :id AND e.active = true")
    Optional<String> findActiveEstablishmentName(@Param("id") UUID establishmentId);

    @Query("""
            SELECT count(t) > 0 FROM DiningTableJpaEntity t
             WHERE t.establishmentId = :establishmentId
               AND lower(t.label) = lower(:label)
               AND t.id <> :exceptId
            """)
    boolean labelExists(@Param("establishmentId") UUID establishmentId, @Param("label") String label,
                        @Param("exceptId") UUID exceptId);

    @Query("""
            SELECT count(t) FROM DiningTableJpaEntity t
             WHERE t.establishmentId = :establishmentId AND t.sectorId = :sectorId AND t.active = true
            """)
    long countActiveBySector(@Param("establishmentId") UUID establishmentId, @Param("sectorId") UUID sectorId);
}
