package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TabRodizioJpaRepository extends JpaRepository<TabRodizioJpaEntity, UUID> {

    Optional<TabRodizioJpaEntity> findByTabIdAndEstablishmentId(UUID tabId, UUID establishmentId);

    /** Grava, a menos que a comanda já tenha um rodízio (o tab_id é único): aí são 0 linhas. */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO tab_rodizio (id, establishment_id, tab_id, rodizio_id, rodizio_name, activated_by, started_at,
                                     ends_at, rodizio_type, status, status_changed_at)
            VALUES (:id, :establishmentId, :tabId, :rodizioId, :rodizioName, :activatedBy, :startedAt, :endsAt,
                    :rodizioType, :status, :statusChangedAt)
            ON CONFLICT (tab_id) DO NOTHING
            """)
    int insertIfNone(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                     @Param("tabId") UUID tabId, @Param("rodizioId") UUID rodizioId,
                     @Param("rodizioName") String rodizioName, @Param("activatedBy") UUID activatedBy,
                     @Param("startedAt") Instant startedAt, @Param("endsAt") Instant endsAt,
                     @Param("rodizioType") String rodizioType, @Param("status") String status,
                     @Param("statusChangedAt") Instant statusChangedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TabRodizioJpaEntity r SET r.status = :status, r.statusChangedAt = :statusChangedAt
             WHERE r.id = :id AND r.establishmentId = :establishmentId
            """)
    int updateStatus(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                     @Param("status") String status, @Param("statusChangedAt") Instant statusChangedAt);

    /**
     * As mesas abertas no rodízio de passagem, numa consulta só: a mesa, o setor e as pessoas
     * (a soma das faixas). As pausadas (cartão vermelho) vêm primeiro: é o que o passador precisa ver.
     */
    @Query(value = """
            SELECT new br.com.cardapio.adapter.out.persistence.rodizio.PassagemTableRow(
                       d.id, d.label, s.name, r.rodizioName,
                       (SELECT SUM(g.quantity) FROM TabRodizioGuestJpaEntity g WHERE g.tabRodizioId = r.id),
                       r.status, r.statusChangedAt)
              FROM TabRodizioJpaEntity r
              JOIN TabJpaEntity t ON t.id = r.tabId
              JOIN DiningTableJpaEntity d ON d.id = t.tableId
              JOIN SectorJpaEntity s ON s.id = d.sectorId
             WHERE r.establishmentId = :establishmentId AND r.rodizioType = 'PASSAGEM'
               AND t.status IN ('OPEN', 'CLOSING')
             ORDER BY CASE WHEN r.status = 'PAUSED' THEN 0 ELSE 1 END, d.label, d.id
            """,
            countQuery = """
            SELECT count(r) FROM TabRodizioJpaEntity r
              JOIN TabJpaEntity t ON t.id = r.tabId
             WHERE r.establishmentId = :establishmentId AND r.rodizioType = 'PASSAGEM'
               AND t.status IN ('OPEN', 'CLOSING')
            """)
    Page<PassagemTableRow> findOpenPassagem(@Param("establishmentId") UUID establishmentId, Pageable pageable);
}
