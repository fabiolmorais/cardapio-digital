package br.com.cardapio.application.service.team;


import br.com.cardapio.application.port.in.team.TeamMemberSummary;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.exception.TeamMemberNotFoundException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListTeamServiceTest {

    private final TeamFakes fakes = new TeamFakes();
    private final ListTeamService service = new ListTeamService(fakes);
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void listsOnlyThisRestaurantTeam() {
        fakes.given(sakura, "Marina", Role.OWNER);
        fakes.given(sakura, "Carlos", Role.STAFF);
        fakes.given(EstablishmentId.newId(), "Bruno", Role.STAFF);

        var page = service.list(sakura, new PageQuery(0, 20));

        assertThat(page.content()).extracting(TeamMemberSummary::name).containsExactly("Carlos", "Marina");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void getFromAnotherRestaurantIsNotFound() {
        var elsewhere = fakes.given(EstablishmentId.newId(), "Bruno", Role.STAFF);

        assertThatThrownBy(() -> service.get(sakura, elsewhere.membership().id()))
                .isInstanceOf(TeamMemberNotFoundException.class);
    }
}