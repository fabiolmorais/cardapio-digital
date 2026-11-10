package br.com.cardapio.adapter.out.persistence.floor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /**
     * O movimento das mesas pedidas: comanda aberta (ou em fechamento), conta pedida (chamado de
     * conta em aberto, ou a comanda em fechamento) e rodízio ativo na comanda aberta.
     */
    @Query(value = """
            SELECT t.id AS tableId,
                   EXISTS (SELECT 1 FROM tab b WHERE b.table_id = t.id AND b.status IN ('OPEN', 'CLOSING')) AS tabOpen,
                   (EXISTS (SELECT 1 FROM call c WHERE c.table_id = t.id AND c.type = 'BILL'
                                AND c.status IN ('PENDING', 'IN_PROGRESS'))
                    OR EXISTS (SELECT 1 FROM tab b WHERE b.table_id = t.id AND b.status = 'CLOSING')) AS billRequested,
                   EXISTS (SELECT 1 FROM tab b JOIN tab_rodizio r ON r.tab_id = b.id
                            WHERE b.table_id = t.id AND b.status IN ('OPEN', 'CLOSING')) AS rodizio
              FROM dining_table t
             WHERE t.establishment_id = :establishmentId AND t.id IN (:tableIds)
            """, nativeQuery = true)
    List<TableActivityRow> findActivity(@Param("establishmentId") UUID establishmentId,
                                                  @Param("tableIds") Collection<UUID> tableIds);
}
