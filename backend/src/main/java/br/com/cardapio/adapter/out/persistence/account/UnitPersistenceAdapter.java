package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.application.port.out.account.UnitPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.Establishment;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.OrganizationId;
import br.com.cardapio.domain.account.model.Slug;
import br.com.cardapio.domain.account.model.UnitMembership;
import br.com.cardapio.domain.account.model.UnitSummary;
import br.com.cardapio.domain.account.model.UserId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * As unidades de uma empresa (RF31). A empresa sai sempre do restaurante de quem pede
 * ({@link #organizationOf}); as consultas filtram por ela.
 */
@Component
class UnitPersistenceAdapter implements UnitPort {

    private final JdbcClient jdbc;
    private final EstablishmentJpaRepository establishments;
    private final MembershipJpaRepository memberships;

    UnitPersistenceAdapter(JdbcClient jdbc, EstablishmentJpaRepository establishments,
                           MembershipJpaRepository memberships) {
        this.jdbc = jdbc;
        this.establishments = establishments;
        this.memberships = memberships;
    }

    @Override
    public OrganizationId organizationOf(EstablishmentId establishmentId) {
        return new OrganizationId(jdbc.sql("SELECT organization_id FROM establishment WHERE id = :id")
                .param("id", establishmentId.value())
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("restaurante sem empresa")));
    }

    @Override
    public long countActive(OrganizationId organizationId) {
        return jdbc.sql("SELECT count(*) FROM establishment WHERE organization_id = :org AND active")
                .param("org", organizationId.value())
                .query(Long.class)
                .single();
    }

    /** As contagens são subconsultas por unidade (índice em establishment_id): uma consulta só, sem N+1. */
    @Override
    public ResultPage<UnitSummary> list(OrganizationId organizationId, PageQuery page) {
        var content = jdbc.sql("""
                        SELECT e.id, e.name, e.slug, e.created_at,
                               (SELECT count(*) FROM dining_table t
                                 WHERE t.establishment_id = e.id AND t.active) AS tables,
                               (SELECT count(*) FROM membership m
                                 WHERE m.establishment_id = e.id AND m.active) AS team
                          FROM establishment e
                         WHERE e.organization_id = :org AND e.active
                         ORDER BY lower(e.name), e.id
                         LIMIT :limit OFFSET :offset
                        """)
                .param("org", organizationId.value())
                .param("limit", page.size())
                .param("offset", (long) page.page() * page.size())
                .query((rs, n) -> new UnitSummary(
                        new EstablishmentId(rs.getObject("id", UUID.class)),
                        rs.getString("name"),
                        new Slug(rs.getString("slug")),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getLong("tables"),
                        rs.getLong("team")))
                .list();
        return new ResultPage<>(content, page.page(), page.size(), countActive(organizationId));
    }

    @Override
    public void saveUnit(Establishment unit, Membership owner) {
        establishments.save(AccountPersistenceMapper.toEntity(unit));
        memberships.save(AccountPersistenceMapper.toEntity(owner));
    }

    @Override
    public List<UnitMembership> unitsFor(OrganizationId organizationId, UserId userId) {
        return jdbc.sql("""
                        SELECT e.id, e.name, e.time_zone,
                               EXISTS (SELECT 1 FROM membership m
                                        WHERE m.establishment_id = e.id AND m.user_id = :user
                                          AND m.active AND m.role = 'OWNER') AS owner
                          FROM establishment e
                         WHERE e.organization_id = :org AND e.active
                         ORDER BY lower(e.name), e.id
                        """)
                .param("org", organizationId.value())
                .param("user", userId.value())
                .query((rs, n) -> new UnitMembership(
                        new EstablishmentId(rs.getObject("id", UUID.class)),
                        rs.getString("name"),
                        ZoneId.of(rs.getString("time_zone")),
                        rs.getBoolean("owner")))
                .list();
    }
}
