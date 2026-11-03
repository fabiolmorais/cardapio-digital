package br.com.cardapio.adapter.out.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    List<OrderJpaEntity> findByEstablishmentIdAndTabIdOrderByCreatedAtAscNumberAsc(UUID establishmentId, UUID tabId);

    long countByEstablishmentIdAndSessionIdAndCreatedAtAfter(UUID establishmentId, UUID sessionId, Instant instant);

    /**
     * O próximo número do dia, numa instrução só: cria a linha do dia com 1, ou soma 1 à que existe.
     * O dia é calculado NO BANCO, no fuso do restaurante (establishment.time_zone). O ON CONFLICT
     * trava a linha do dia: dois pedidos juntos recebem números diferentes.
     */
    @Query(nativeQuery = true, value = """
            INSERT INTO order_counter (establishment_id, day, last_number)
            SELECT e.id, (CAST(:now AS TIMESTAMPTZ) AT TIME ZONE e.time_zone)::date, 1
              FROM establishment e
             WHERE e.id = :establishmentId
            ON CONFLICT (establishment_id, day) DO UPDATE SET last_number = order_counter.last_number + 1
            RETURNING last_number
            """)
    int nextNumber(@Param("establishmentId") UUID establishmentId, @Param("now") Instant now);
}
