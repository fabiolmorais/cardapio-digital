package br.com.cardapio.application.service.team;


import br.com.cardapio.application.port.in.team.AddTeamMemberCommand;
import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.domain.account.exception.TeamPermissionException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddTeamMemberServiceTest {

    private final TeamFakes fakes = new TeamFakes();
    private final AddTeamMemberService service = new AddTeamMemberService(fakes, fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    private AddTeamMemberCommand command(Role actor, String name, Role role, String pin) {
        return new AddTeamMemberCommand(sakura, actor, name, role, pin);
    }

    @Test
    void ownerAddsAWaiterWithHashedPin() {
        TeamMemberSummary carlos = service.add(command(Role.OWNER, " Carlos ", Role.STAFF, "258014"));

        assertThat(carlos.name()).isEqualTo("Carlos");
        assertThat(carlos.role()).isEqualTo(Role.STAFF);
        assertThat(carlos.active()).isTrue();
        var stored = fakes.stored(carlos.id());
        assertThat(stored.establishmentId()).isEqualTo(sakura);
        assertThat(stored.pinHash().value()).isEqualTo("hash:258014"); // nunca o PIN puro
    }

    @Test
    void managerAddsKitchenButNotAnotherManager() {
        assertThat(service.add(command(Role.MANAGER, "Rita", Role.KITCHEN, "258014")).role()).isEqualTo(Role.KITCHEN);

        assertThatThrownBy(() -> service.add(command(Role.MANAGER, "Ana", Role.MANAGER, "258014")))
                .isInstanceOf(TeamPermissionException.class);
    }

    @Test
    void nobodyAddsAnotherOwner() {
        assertThatThrownBy(() -> service.add(command(Role.OWNER, "Ana", Role.OWNER, "258014")))
                .isInstanceOf(TeamPermissionException.class);
        assertThat(fakes.members).isEmpty();
    }

    @Test
    void weakPinIsRefusedAndNothingIsSaved() {
        assertThatThrownBy(() -> service.add(command(Role.OWNER, "Carlos", Role.STAFF, "123456")))
                .isInstanceOf(InvalidValueException.class);
        assertThat(fakes.members).isEmpty();
    }
}