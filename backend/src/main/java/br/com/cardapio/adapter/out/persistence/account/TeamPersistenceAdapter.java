package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Equipe de um restaurante. Defesa em dobro: toda consulta tem o "WHERE establishment_id"
 * e, antes dela, o filtro do Hibernate é ligado para o mesmo restaurante.
 */
@Component
class TeamPersistenceAdapter implements TeamMemberPort {


    private final UserJpaRepository users;
    private final MembershipJpaRepository memberships;
    private final EstablishmentFilter establishmentFilter;

    TeamPersistenceAdapter(UserJpaRepository users, MembershipJpaRepository memberships,
                           EstablishmentFilter establishmentFilter) {
        this.users = users;
        this.memberships = memberships;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public ResultPage<TeamMember> findTeam(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<TeamMemberRow> rows = memberships.findTeam(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(AccountPersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public ResultPage<TeamMember> findPinMembers(EstablishmentId establishmentId, PageQuery page) {
        establishmentFilter.enable(establishmentId);
        Page<TeamMemberRow> rows = memberships.findPinMembers(establishmentId.value(),
                PageRequest.of(page.page(), page.size()));
        return new ResultPage<>(rows.map(AccountPersistenceMapper::toDomain).getContent(),
                page.page(), page.size(), rows.getTotalElements());
    }

    @Override
    public Optional<TeamMember> findMember(EstablishmentId establishmentId, MembershipId memberId) {
        establishmentFilter.enable(establishmentId);
        return memberships.findTeamMember(establishmentId.value(), memberId.value())
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public void addMember(User user, Membership membership) {
        establishmentFilter.enable(membership.establishmentId());
        users.save(AccountPersistenceMapper.toEntity(user));
        memberships.save(AccountPersistenceMapper.toEntity(membership));
    }

    @Override
    public void updateMember(Membership membership) {
        establishmentFilter.enable(membership.establishmentId());
        memberships.save(AccountPersistenceMapper.toEntity(membership));
    }
}
