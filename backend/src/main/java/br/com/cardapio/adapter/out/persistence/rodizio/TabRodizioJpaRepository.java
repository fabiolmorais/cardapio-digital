package br.com.cardapio.adapter.out.persistence.rodizio;

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
            INSERT INTO tab_rodizio (id, establishment_id, tab_id, rodizio_id, rodizio_name, activated_by, started_at)
            VALUES (:id, :establishmentId, :tabId, :rodizioId, :rodizioName, :activatedBy, :startedAt)
            ON CONFLICT (tab_id) DO NOTHING
            """)
    int insertIfNone(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId,
                     @Param("tabId") UUID tabId, @Param("rodizioId") UUID rodizioId,
                     @Param("rodizioName") String rodizioName, @Param("activatedBy") UUID activatedBy,
                     @Param("startedAt") Instant startedAt);
}
