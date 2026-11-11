package br.com.cardapio.adapter.out.persistence.tablesession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface TableSessionJpaRepository extends JpaRepository<TableSessionJpaEntity, UUID> {

    Optional<TableSessionJpaEntity> findByTokenHash(String tokenHash);

    /**
     * "Liberar": a sessão de quem pediu ganha a presença do garçom, se ainda estiver ativa e sem
     * presença. A bloqueada pela equipe fica de fora.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TableSessionJpaEntity s
               SET s.presenceMethod = 'STAFF', s.presenceConfirmedAt = :now, s.unlockedBy = :by
             WHERE s.id = :id AND s.establishmentId = :establishmentId
               AND s.presenceMethod IS NULL AND s.blockedAt IS NULL AND s.endedAt IS NULL AND s.expiresAt > :now
            """)
    int unlockSession(@Param("establishmentId") UUID establishmentId, @Param("id") UUID id,
                      @Param("by") UUID by, @Param("now") Instant now);

    /** "Desbloquear este celular": 1 se desbloqueou, 0 se não é do restaurante ou não estava bloqueada. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TableSessionJpaEntity s SET s.blockedAt = NULL, s.blockedBy = NULL
             WHERE s.id = :id AND s.establishmentId = :establishmentId AND s.blockedAt IS NOT NULL
            """)
    int unblock(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId);

    @Query("""
            SELECT min(s.createdAt) FROM TableSessionJpaEntity s
             WHERE s.establishmentId = :establishmentId AND s.tableId = :tableId
               AND s.endedAt IS NULL AND s.expiresAt > :now
            """)
    Optional<Instant> findVisitStart(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId,
                                     @Param("now") Instant now);

    @Query("SELECT s.tableId FROM TableSessionJpaEntity s WHERE s.id = :id AND s.establishmentId = :establishmentId")
    Optional<UUID> findTableId(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId);

    /** Os bloqueados com a sessão aberta, com a mesa, o setor e quem bloqueou (LEFT: a pessoa pode ter saído). */
    @Query(value = """
            SELECT s.id AS id, s.tableId AS tableId, t.label AS tableLabel, sc.name AS sectorName,
                   s.blockedAt AS blockedAt, u.name AS blockedByName
              FROM TableSessionJpaEntity s
              JOIN DiningTableJpaEntity t ON t.id = s.tableId
              JOIN SectorJpaEntity sc ON sc.id = t.sectorId
              LEFT JOIN UserJpaEntity u ON u.id = s.blockedBy
             WHERE s.establishmentId = :establishmentId AND s.blockedAt IS NOT NULL
               AND s.endedAt IS NULL AND s.expiresAt > :now
             ORDER BY s.blockedAt DESC, s.id
            """,
            countQuery = """
            SELECT count(s) FROM TableSessionJpaEntity s
             WHERE s.establishmentId = :establishmentId AND s.blockedAt IS NOT NULL
               AND s.endedAt IS NULL AND s.expiresAt > :now
            """)
    Page<BlockedPhoneRow> findBlocked(@Param("establishmentId") UUID establishmentId, @Param("now") Instant now,
                                      Pageable pageable);

    /** "Bloquear este celular": 1 se bloqueou, 0 se não é do restaurante ou já estava bloqueada. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TableSessionJpaEntity s SET s.blockedAt = :now, s.blockedBy = :by
             WHERE s.id = :id AND s.establishmentId = :establishmentId AND s.blockedAt IS NULL
            """)
    int block(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId, @Param("by") UUID by,
              @Param("now") Instant now);

    /**
     * Encerra as sessões vencidas, de todos os restaurantes (é uma tarefa do sistema). A sessão
     * "acabou" quando venceu, e não quando a tarefa passou: por isso o ended_at recebe o expires_at.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TableSessionJpaEntity s SET s.endedAt = s.expiresAt WHERE s.endedAt IS NULL AND s.expiresAt <= :now")
    int endExpired(@Param("now") Instant now);

    /** A comanda fechou: encerra as sessões ainda abertas da mesa, num UPDATE só. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TableSessionJpaEntity s SET s.endedAt = :now
             WHERE s.establishmentId = :establishmentId AND s.tableId = :tableId AND s.endedAt IS NULL
            """)
    int endTable(@Param("establishmentId") UUID establishmentId, @Param("tableId") UUID tableId,
                 @Param("now") Instant now);
}
