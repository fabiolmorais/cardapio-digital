package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    long countByUserIdAndCreatedAtGreaterThanEqual(UUID userId, Instant since);

    /** Gasta de uma vez os links ainda válidos da pessoa (depois de trocar a senha). */
    @Modifying
    @Query("""
            UPDATE PasswordResetTokenJpaEntity t
               SET t.usedAt = :now
             WHERE t.userId = :userId
               AND t.usedAt IS NULL
            """)
    int useAllOf(@Param("userId") UUID userId, @Param("now") Instant now);
}
