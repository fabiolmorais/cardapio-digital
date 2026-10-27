package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.in.team.UpdateTeamMemberCommand;
import br.com.cardapio.application.port.in.team.UpdateTeamMemberUseCase;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.application.port.out.team.TeamMemberPort;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.PinPolicy;
import br.com.cardapio.domain.account.service.TeamPolicy;
import org.springframework.transaction.annotation.Transactional;

public class UpdateTeamMemberService implements UpdateTeamMemberUseCase {

    private final TeamMemberPort team;
    private final PasswordHasher hasher;

    public UpdateTeamMemberService(TeamMemberPort team, PasswordHasher hasher) {
        this.team = team;
        this.hasher = hasher;
    }

    @Override
    @Transactional
    public TeamMemberSummary update(UpdateTeamMemberCommand command) {
        TeamMember current = team.findMember(command.establishmentId(), command.memberId())
                .orElseThrow(TeamMemberNotFoundException::new);
        Membership membership = current.membership();

        // Precisa poder mexer em quem a pessoa é hoje…
        TeamPolicy.checkCanManage(command.actorRole(), membership.role());
        if (command.newRole() != null && command.newRole() != membership.role()) {
            // …e no que ela vai virar (gerente não promove ninguém a gerente).
            TeamPolicy.checkCanManage(command.actorRole(), command.newRole());
            membership = membership.changeRole(command.newRole());
        }
        if (command.newPin() != null) {
            PinPolicy.check(command.newPin());
            membership = membership.changePin(hasher.hash(command.newPin()));
        }

        team.updateMember(membership);
        return TeamMembers.summary(new TeamMember(membership, current.name()));
    }
}
