package br.com.cardapio.adapter.out.persistence.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /** Vínculo + nome da pessoa numa consulta só (sem N+1). */
    String TEAM_MEMBER = """
            SELECT  m.id as id,
                    m.userId as userId,
                    m.establishmentId as establishmentId,
                    m.role as role,
                    m.pinHash as pinHash,
                    m.active as active,
                    u.name as name
            FROM MembershipJpaEntity m
            JOIN UserJpaEntity u ON u.id = m.userId
            WHERE m.establishmentId = :establishmentId
            """;

    /** Ativos primeiro, depois por nome; o id desempata para a paginação ficar estável. */
    @Query(value = TEAM_MEMBER + " ORDER BY m.active DESC, lower(u.name), m.id",
            countQuery = "SELECT count(m) FROM MembershipJpaEntity m WHERE m.establishmentId = :establishmentId")
    Page<TeamMemberRow> findTeam(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    /** Quem pode entrar por PIN: ativos e que não são o dono. */
    @Query(value = TEAM_MEMBER + " AND m.active = true AND m.role <> 'OWNER' ORDER BY lower(u.name), m.id",
            countQuery = """
                    SELECT count(m) FROM MembershipJpaEntity m
                     WHERE m.establishmentId = :establishmentId AND m.active = true AND m.role <> 'OWNER'
                    """)
    Page<TeamMemberRow> findPinMembers(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    /**
     * SELECT … FOR UPDATE: trava a linha do vínculo até o fim da transação. Outra transação que
     * pedir a mesma trava espera aqui. Usado para contar as tentativas de PIN sem perder nenhuma.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MembershipJpaEntity m WHERE m.id = :id AND m.establishmentId = :establishmentId")
    Optional<MembershipJpaEntity> lockById(@Param("id") UUID id, @Param("establishmentId") UUID establishmentId);

    @Query(TEAM_MEMBER + " AND m.id = :id")
    Optional<TeamMemberRow> findTeamMember(@Param("establishmentId") UUID establishmentId, @Param("id") UUID id);
}
