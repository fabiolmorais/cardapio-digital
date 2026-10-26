package br.com.cardapio.adapter.out.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, UUID> {

    String ACTIVE_MEMBERSHIP = """
            SELECT  m.id as membershipId,
                    e.id as establishmentId,
                    e.name as establishmentName,
                    e.slug as establishmentSlug,
                    m.role as role
            FROM MembershipJpaEntity m, EstablishmentJpaEntity e
            WHERE e.id = m.establishmentId
                AND m.active = true
                AND e.active = true
                AND m.userId = :userId
            """;

    @Query(ACTIVE_MEMBERSHIP + " order by e.name")
    List<ActiveMembershipRow> findActiveByUser(@Param("userId") UUID userId);

    @Query(ACTIVE_MEMBERSHIP + " and e.id = :establishmentId")
    Optional<ActiveMembershipRow> findActive(@Param("userId") UUID userId, @Param("establishmentId") UUID establishmentId);
}
