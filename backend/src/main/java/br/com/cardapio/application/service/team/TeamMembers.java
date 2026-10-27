package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.domain.account.model.Membership;

/** Converte o dado da porta de saída no resultado da porta de entrada. */
final class TeamMembers {

    private TeamMembers() {
    }

    static TeamMemberSummary summary(TeamMember member) {
        Membership m = member.membership();
        return new TeamMemberSummary(m.id(), member.name(), m.role(), m.active());
    }
}
