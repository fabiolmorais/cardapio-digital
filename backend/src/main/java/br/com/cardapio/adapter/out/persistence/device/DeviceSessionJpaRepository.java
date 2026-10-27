package br.com.cardapio.adapter.out.persistence.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

interface DeviceSessionJpaRepository extends JpaRepository<DeviceSessionJpaEntity, UUID> {

    /**
     * Um UPDATE só para todas as sessões do aparelho: revoga os refresh tokens ativos das
     * famílias abertas nele. RefreshTokenJpaEntity mora em persistence.account; o JPQL a
     * encontra pelo nome da entidade, como no Guia 5.
     */
    @Modifying
    @Query("""
            UPDATE RefreshTokenJpaEntity t
               SET t.revokedAt = :now
             WHERE t.revokedAt IS NULL
               AND t.familyId IN (SELECT s.familyId FROM DeviceSessionJpaEntity s WHERE s.deviceId = :deviceId)
            """)
    int revokeAll(@Param("deviceId") UUID deviceId, @Param("now") Instant now);
}
