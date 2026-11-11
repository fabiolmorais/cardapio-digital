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
