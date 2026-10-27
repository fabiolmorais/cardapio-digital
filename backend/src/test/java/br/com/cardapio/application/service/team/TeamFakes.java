package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.*;

import java.util.*;

/** Equipe em memória e um "hash" previsível, para testar os casos de uso sem banco e sem Spring. */
final class TeamFakes implements TeamMemberPort, PasswordHasher {

    final Map<MembershipId, TeamMember> members = new LinkedHashMap<>();

    /** Cadastra direto no fake (sem passar pelas regras), para montar o cenário do teste. */
    TeamMember given(EstablishmentId establishmentId, String name, Role role) {
        User user = User.teamMember(name);
        Membership membership = role == Role.OWNER
                ? Membership.owner(user.id(), establishmentId)
                : Membership.teamMember(user.id(), establishmentId, role, hash("2580"));
        TeamMember member = new TeamMember(membership, name);
        members.put(membership.id(), member);
        return member;
    }

    Membership stored(MembershipId id) {
        return members.get(id).membership();
    }

    // TeamMemberPort
    @Override
    public ResultPage<TeamMember> findTeam(EstablishmentId establishmentId, PageQuery page) {
        List<TeamMember> all = members.values().stream()
                .filter(m -> m.membership().establishmentId().equals(establishmentId))
                .sorted(Comparator.comparing((TeamMember m) -> !m.membership().active())
                        .thenComparing(TeamMember::name))
                .toList();
        List<TeamMember> content = all.stream()
                .skip((long) page.page() * page.size()).limit(page.size()).toList();
        return new ResultPage<>(content, page.page(), page.size(), all.size());
    }

    @Override
    public Optional<TeamMember> findMember(EstablishmentId establishmentId, MembershipId memberId) {
        return Optional.ofNullable(members.get(memberId))
                .filter(m -> m.membership().establishmentId().equals(establishmentId));
    }

    @Override
    public void addMember(User user, Membership membership) {
        members.put(membership.id(), new TeamMember(membership, user.name()));
    }

    @Override
    public void updateMember(Membership membership) {
        members.computeIfPresent(membership.id(), (id, old) -> new TeamMember(membership, old.name()));
    }

    // PasswordHasher
    @Override
    public PasswordHash hash(String raw) { return new PasswordHash("hash:" + raw); }

    @Override
    public boolean matches(String raw, PasswordHash hash) { return hash.value().equals("hash:" + raw); }
}
