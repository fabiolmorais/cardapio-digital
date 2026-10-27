package br.com.cardapio.application.service.team;


import br.com.cardapio.application.port.in.team.UpdateTeamMemberCommand;
import br.com.cardapio.application.port.out.team.TeamMember;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.exception.TeamPermissionException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.MembershipId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateTeamMemberServiceTest {

    private final TeamFakes fakes = new TeamFakes();
    private final UpdateTeamMemberService service = new UpdateTeamMemberService(fakes, fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    private UpdateTeamMemberCommand command(Role actor, MembershipId id, Role newRole, String newPin) {
        return new UpdateTeamMemberCommand(sakura, actor, id, newRole, newPin);
    }

    @Test
    void ownerChangesRoleAndPin() {
        TeamMember carlos = fakes.given(sakura, "Carlos", Role.STAFF);

        var updated = service.update(command(Role.OWNER, carlos.membership().id(), Role.MANAGER, "739105"));

        assertThat(updated.role()).isEqualTo(Role.MANAGER);
        assertThat(fakes.stored(carlos.membership().id()).pinHash().value()).isEqualTo("hash:739105");
    }

    @Test
    void nullFieldsAreLeftAsTheyAre() {
        TeamMember carlos = fakes.given(sakura, "Carlos", Role.STAFF);

        var updated = service.update(command(Role.OWNER, carlos.membership().id(), null, null));

        assertThat(updated.role()).isEqualTo(Role.STAFF);
        assertThat(fakes.stored(carlos.membership().id()).pinHash().value()).isEqualTo("hash:258014");
    }

    @Test
    void managerCannotPromoteToManagerNorTouchAManager() {
        TeamMember carlos = fakes.given(sakura, "Carlos", Role.STAFF);
        TeamMember ana = fakes.given(sakura, "Ana", Role.MANAGER);

        assertThatThrownBy(() -> service.update(command(Role.MANAGER, carlos.membership().id(), Role.MANAGER, null)))
                .isInstanceOf(TeamPermissionException.class);
        assertThatThrownBy(() -> service.update(command(Role.MANAGER, ana.membership().id(), null, "739105")))
                .isInstanceOf(TeamPermissionException.class);
        assertThat(fakes.stored(carlos.membership().id()).role()).isEqualTo(Role.STAFF);
    }

    @Test
    void ownerCannotBeChanged() {
        TeamMember marina = fakes.given(sakura, "Marina", Role.OWNER);

        assertThatThrownBy(() -> service.update(command(Role.OWNER, marina.membership().id(), Role.MANAGER, null)))
                .isInstanceOf(TeamPermissionException.class);
    }

    @Test
    void weakPinIsRefused() {
        TeamMember carlos = fakes.given(sakura, "Carlos", Role.STAFF);

        assertThatThrownBy(() -> service.update(command(Role.OWNER, carlos.membership().id(), null, "000000")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void memberOfAnotherRestaurantIsNotFound() {
        TeamMember elsewhere = fakes.given(EstablishmentId.newId(), "Bruno", Role.STAFF);

        assertThatThrownBy(() -> service.update(command(Role.OWNER, elsewhere.membership().id(), Role.KITCHEN, null)))
                .isInstanceOf(TeamMemberNotFoundException.class);
        assertThat(fakes.stored(elsewhere.membership().id()).role()).isEqualTo(Role.STAFF);
    }
}