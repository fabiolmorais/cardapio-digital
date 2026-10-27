package br.com.cardapio.application.service.team;

import br.com.cardapio.application.port.in.team.ReactivateTeamMemberCommand;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.exception.TeamPermissionException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactivateTeamMemberServiceTest {

    private final TeamFakes fakes = new TeamFakes();
    private final ReactivateTeamMemberService service = new ReactivateTeamMemberService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    private TeamMember deactivated(EstablishmentId establishmentId, String name, Role role) {
        TeamMember member = fakes.given(establishmentId, name, role);
        fakes.updateMember(member.membership().deactivate());
        return member;
    }

    @Test
    void managerReactivatesAWaiterAndDoingItTwiceIsFine() {
        TeamMember carlos = deactivated(sakura, "Carlos", Role.STAFF);
        var command = new ReactivateTeamMemberCommand(sakura, Role.MANAGER, carlos.membership().id());

        var back = service.reactivate(command);
        service.reactivate(command);

        assertThat(back.active()).isTrue();
        assertThat(back.role()).isEqualTo(Role.STAFF);
        assertThat(fakes.stored(carlos.membership().id()).active()).isTrue();
    }

    @Test
    void managerCannotReactivateAManager() {
        TeamMember ana = deactivated(sakura, "Ana", Role.MANAGER);

        assertThatThrownBy(() -> service.reactivate(
                new ReactivateTeamMemberCommand(sakura, Role.MANAGER, ana.membership().id())))
                .isInstanceOf(TeamPermissionException.class);
        assertThat(fakes.stored(ana.membership().id()).active()).isFalse();
    }

    @Test
    void memberOfAnotherRestaurantIsNotFound() {
        TeamMember elsewhere = deactivated(EstablishmentId.newId(), "Bruno", Role.STAFF);

        assertThatThrownBy(() -> service.reactivate(
                new ReactivateTeamMemberCommand(sakura, Role.OWNER, elsewhere.membership().id())))
                .isInstanceOf(TeamMemberNotFoundException.class);
        assertThat(fakes.stored(elsewhere.membership().id()).active()).isFalse();
    }
}
