package br.com.cardapio.adapter.out.persistence.tablesession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TableSessionJpaRepository extends JpaRepository<TableSessionJpaEntity, UUID> {

    Optional<TableSessionJpaEntity> findByTokenHash(String tokenHash);

    /** "Liberar esta mesa": as sessões ativas da mesa sem presença ganham a do garçom, num UPDATE só. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TableSessionJpaEntity s
               SET s.presenceMethod = 'STAFF', s.presenceConfirmedAt = :now, s.unlockedBy = :by
             WHERE s.establishmentId = :establishmentId AND s.tableId = :tableId
               AND s.presenceMethod IS NULL AND s.endedAt IS NULL AND s.expiresAt > :now
            """)
    int unlockTable(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId,
                    @Param("by") UUID by, @Param("now") Instant now);
}
