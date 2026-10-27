package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.ListTeamUseCase;
import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import org.springframework.transaction.annotation.Transactional;

public class ListTeamService implements ListTeamUseCase {

    private final TeamMemberPort team;

    public ListTeamService(TeamMemberPort team) {
        this.team = team;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<TeamMemberSummary> list(EstablishmentId establishmentId, PageQuery page) {
        return team.findTeam(establishmentId, page).map(TeamMembers::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamMemberSummary get(EstablishmentId establishmentId, MembershipId memberId) {
        return team.findMember(establishmentId, memberId)
                .map(TeamMembers::summary)
                .orElseThrow(TeamMemberNotFoundException::new);
    }
}
