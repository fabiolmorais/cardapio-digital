package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.DeactivateTeamMemberCommand;
import br.com.cardapio.application.port.in.team.DeactivateTeamMemberUseCase;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.service.TeamPolicy;
import org.springframework.transaction.annotation.Transactional;

public class DeactivateTeamMemberService implements DeactivateTeamMemberUseCase {

    private final TeamMemberPort team;

    public DeactivateTeamMemberService(TeamMemberPort team) {
        this.team = team;
    }

    @Override
    @Transactional
    public void deactivate(DeactivateTeamMemberCommand command) {
        TeamMember current = team.findMember(command.establishmentId(), command.memberId())
                .orElseThrow(TeamMemberNotFoundException::new);
        TeamPolicy.checkCanManage(command.actorRole(), current.membership().role());

        if (current.membership().active()) {
            team.updateMember(current.membership().deactivate());
        }
    }
}
