package br.com.cardapio.adapter.out.persistence.rodizio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RodizioPhoneJpaRepository extends JpaRepository<RodizioPhoneJpaEntity, UUID> {

    Optional<RodizioPhoneJpaEntity> findByEstablishmentIdAndTabRodizioIdAndSessionId(UUID establishmentId,
                                                                                  UUID tabRodizioId,
                                                                                  UUID sessionId);

    /** Os celulares ativos da mesa entram; quem já estava (ou tinha pedido) passa a estar dentro. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO rodizio_phone (id, establishment_id, tab_rodizio_id, session_id, joined_at)
            SELECT gen_random_uuid(), s.establishment_id, :rodizio, s.id, :now
              FROM table_session s
             WHERE s.establishment_id = :establishmentId AND s.table_id = :tableId
               AND s.ended_at IS NULL AND s.expires_at > :now
            ON CONFLICT (tab_rodizio_id, session_id)
            DO UPDATE SET joined_at = COALESCE(rodizio_phone.joined_at, EXCLUDED.joined_at)
            """, nativeQuery = true)
    int joinAllAtTable(@Param("establishmentId") UUID establishmentId, @Param("rodizio") UUID rodizio,
                       @Param("tableId") UUID tableId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO rodizio_phone (id, establishment_id, tab_rodizio_id, session_id, requested_at)
            VALUES (gen_random_uuid(), :establishmentId, :rodizio, :session, :now)
            ON CONFLICT (tab_rodizio_id, session_id)
            DO UPDATE SET requested_at = COALESCE(rodizio_phone.requested_at, EXCLUDED.requested_at)
            """, nativeQuery = true)
    int request(@Param("establishmentId") UUID establishmentId, @Param("rodizio") UUID rodizio,
                @Param("session") UUID session, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE rodizio_phone SET joined_at = :now
             WHERE establishment_id = :establishmentId AND tab_rodizio_id = :rodizio
               AND requested_at IS NOT NULL AND joined_at IS NULL
            """, nativeQuery = true)
    int admitRequested(@Param("establishmentId") UUID establishmentId, @Param("rodizio") UUID rodizio,
                       @Param("now") Instant now);
}
