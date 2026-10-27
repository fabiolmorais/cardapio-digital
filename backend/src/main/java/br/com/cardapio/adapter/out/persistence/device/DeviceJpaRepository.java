package br.com.cardapio.adapter.out.persistence.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface DeviceJpaRepository extends JpaRepository<DeviceJpaEntity, UUID> {

    @Query(value = """
            SELECT d FROM DeviceJpaEntity d
             WHERE d.establishmentId = :establishmentId
               AND d.revokedAt IS NULL
             ORDER BY lower(d.name), d.id
            """,
            countQuery = """
            SELECT count(d) FROM DeviceJpaEntity d
             WHERE d.establishmentId = :establishmentId
               AND d.revokedAt IS NULL
            """)
    Page<DeviceJpaEntity> findActive(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    Optional<DeviceJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    Optional<DeviceJpaEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    /** Nome do restaurante, para o aparelho mostrar na tela depois de vinculado. */
    @Query("SELECT e.name FROM EstablishmentJpaEntity e WHERE e.id = :id")
    Optional<String> findEstablishmentName(@Param("id") UUID establishmentId);
}
