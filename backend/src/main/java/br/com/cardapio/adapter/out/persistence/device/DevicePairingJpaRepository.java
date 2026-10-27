package br.com.cardapio.adapter.out.persistence.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface DevicePairingJpaRepository extends JpaRepository<DevicePairingJpaEntity, UUID> {

    Optional<DevicePairingJpaEntity> findFirstByCodeAndDeviceIdIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String code, Instant now);

    /**
     * Liga o pedido ao aparelho só se ainda estiver aberto. flushAutomatically: o aparelho
     * acabou de ser gravado nesta transação e precisa chegar ao banco antes deste UPDATE,
     * senão a chave estrangeira device_id apontaria para uma linha que o banco ainda não tem.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE DevicePairingJpaEntity p
               SET p.deviceId = :deviceId
             WHERE p.id = :id
               AND p.deviceId IS NULL
               AND p.expiresAt > :now
            """)
    int linkIfOpen(@Param("id") UUID id, @Param("deviceId") UUID deviceId, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE DevicePairingJpaEntity p
               SET p.claimedAt = :now
             WHERE p.id = :id
               AND p.deviceId IS NOT NULL
               AND p.claimedAt IS NULL
            """)
    int claimIfLinked(@Param("id") UUID id, @Param("now") Instant now);
}
