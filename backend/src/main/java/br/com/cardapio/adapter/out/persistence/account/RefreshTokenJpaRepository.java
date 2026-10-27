package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    /**
     * Revoga um token só se ele ainda estiver ativo. Se duas renovações chegarem juntas, o
     * PostgreSQL trava a linha: a segunda espera a primeira terminar, vê revoked_at preenchido
     * e não altera nada (devolve 0).
     */
    @Modifying
    @Query("""
            UPDATE RefreshTokenJpaEntity t
               SET t.revokedAt = :now
             WHERE t.id = :id
               AND t.revokedAt IS NULL
            """)
    int revokeIfActive(@Param("id") UUID id, @Param("now") Instant now);

    /** Um UPDATE só, para a família inteira. Só mexe nos que ainda estão ativos. */
    @Modifying
    @Query("""
            UPDATE RefreshTokenJpaEntity t
               SET t.revokedAt = :now
             WHERE t.familyId = :familyId
               AND t.revokedAt IS NULL
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
