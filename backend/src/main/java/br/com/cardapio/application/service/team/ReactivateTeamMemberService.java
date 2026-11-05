package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.ReactivateTeamMemberCommand;
import br.com.cardapio.application.port.in.team.ReactivateTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.service.TeamPolicy;
import br.com.cardapio.domain.plan.model.PlanResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Religar mantém o mesmo registro: o histórico de pedidos e chamados continua ligado à pessoa.
 * Religar ocupa uma vaga do plano, como cadastrar alguém novo (Etapa 14).
 */
public class ReactivateTeamMemberService implements ReactivateTeamMemberUseCase {

    private final TeamMemberPort team;
    private final SubscriptionPort subscriptions;

    public ReactivateTeamMemberService(TeamMemberPort team, SubscriptionPort subscriptions) {
        this.team = team;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional
    public TeamMemberSummary reactivate(ReactivateTeamMemberCommand command) {
        TeamMember current = team.findMember(command.establishmentId(), command.memberId())
                .orElseThrow(TeamMemberNotFoundException::new);
        TeamPolicy.checkCanManage(command.actorRole(), current.membership().role());

        Membership membership = current.membership();
        if (!membership.active()) {
            PlanAllowances.requireRoomForOneMore(subscriptions, command.establishmentId(), PlanResource.USERS);
            membership = membership.reactivate();
            team.updateMember(membership);
        }
        return TeamMembers.summary(new TeamMember(membership, current.name()));
    }
}
