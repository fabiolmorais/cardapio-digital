package br.com.cardapio.application.service.team;


import br.com.cardapio.application.port.in.team.DeactivateTeamMemberCommand;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.exception.TeamPermissionException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeactivateTeamMemberServiceTest {

    private final TeamFakes fakes = new TeamFakes();
    private final DeactivateTeamMemberService service = new DeactivateTeamMemberService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void managerDeactivatesAWaiterAndDoingItTwiceIsFine() {
        TeamMember carlos = fakes.given(sakura, "Carlos", Role.STAFF);
        var command = new DeactivateTeamMemberCommand(sakura, Role.MANAGER, carlos.membership().id());

        service.deactivate(command);
        service.deactivate(command);

        assertThat(fakes.stored(carlos.membership().id()).active()).isFalse();
    }

    @Test
    void ownerCannotBeDeactivated() {
        TeamMember marina = fakes.given(sakura, "Marina", Role.OWNER);

        assertThatThrownBy(() -> service.deactivate(
                new DeactivateTeamMemberCommand(sakura, Role.OWNER, marina.membership().id())))
                .isInstanceOf(TeamPermissionException.class);
        assertThat(fakes.stored(marina.membership().id()).active()).isTrue();
    }

    @Test
    void memberOfAnotherRestaurantIsNotFound() {
        TeamMember elsewhere = fakes.given(EstablishmentId.newId(), "Bruno", Role.STAFF);

        assertThatThrownBy(() -> service.deactivate(
                new DeactivateTeamMemberCommand(sakura, Role.OWNER, elsewhere.membership().id())))
                .isInstanceOf(TeamMemberNotFoundException.class);
        assertThat(fakes.stored(elsewhere.membership().id()).active()).isTrue();
    }
}