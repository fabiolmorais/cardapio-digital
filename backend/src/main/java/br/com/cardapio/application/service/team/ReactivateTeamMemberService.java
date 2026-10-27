package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.ReactivateTeamMemberCommand;
import br.com.cardapio.application.port.in.team.ReactivateTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.service.TeamPolicy;
import org.springframework.transaction.annotation.Transactional;

/**
 * Religar mantém o mesmo registro: o histórico de pedidos e chamados continua ligado à pessoa.
 * Quando o limite de usuários do plano entrar (Etapa 14), ele vale aqui também.
 */
public class ReactivateTeamMemberService implements ReactivateTeamMemberUseCase {

    private final TeamMemberPort team;

    public ReactivateTeamMemberService(TeamMemberPort team) {
        this.team = team;
    }

    @Override
    @Transactional
    public TeamMemberSummary reactivate(ReactivateTeamMemberCommand command) {
        TeamMember current = team.findMember(command.establishmentId(), command.memberId())
                .orElseThrow(TeamMemberNotFoundException::new);
        TeamPolicy.checkCanManage(command.actorRole(), current.membership().role());

        Membership membership = current.membership();
        if (!membership.active()) {
            membership = membership.reactivate();
            team.updateMember(membership);
        }
        return TeamMembers.summary(new TeamMember(membership, current.name()));
    }
}
