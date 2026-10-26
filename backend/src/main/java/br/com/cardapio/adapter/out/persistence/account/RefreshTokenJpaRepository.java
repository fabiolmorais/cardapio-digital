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
