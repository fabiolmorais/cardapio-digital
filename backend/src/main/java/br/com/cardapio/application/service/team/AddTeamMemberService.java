package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.AddTeamMemberCommand;
import br.com.cardapio.application.port.in.team.AddTeamMemberUseCase;
import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.application.service.plan.PlanAllowances;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.PinPolicy;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.account.service.TeamPolicy;
import br.com.cardapio.domain.plan.model.PlanResource;
import org.springframework.transaction.annotation.Transactional;

public class AddTeamMemberService implements AddTeamMemberUseCase {

    private final TeamMemberPort team;
    private final PasswordHasher hasher;
    private final SubscriptionPort subscriptions;

    public AddTeamMemberService(TeamMemberPort team, PasswordHasher hasher, SubscriptionPort subscriptions) {
        this.team = team;
        this.hasher = hasher;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional
    public TeamMemberSummary add(AddTeamMemberCommand command) {
        TeamPolicy.checkCanManage(command.actorRole(), command.role());
        PinPolicy.check(command.pin());
        PlanAllowances.requireRoomForOneMore(subscriptions, command.establishmentId(), PlanResource.USERS);

        User user = User.teamMember(command.name());
        Membership membership = Membership.teamMember(user.id(), command.establishmentId(), command.role(),
                hasher.hash(command.pin()));
        team.addMember(user, membership);

        return TeamMembers.summary(new TeamMember(membership, user.name()));
    }
}
