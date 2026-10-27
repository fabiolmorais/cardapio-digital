package br.com.cardapio.domain.account.model;


import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipTest {

    private static final PasswordHash PIN = new PasswordHash("hash:2580");
    private final UserId carlos = UserId.newId();
    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void teamMemberStartsActiveWithPin() {
        Membership waiter = Membership.teamMember(carlos, sakura, Role.STAFF, PIN);

        assertThat(waiter.role()).isEqualTo(Role.STAFF);
        assertThat(waiter.pinHash()).isEqualTo(PIN);
        assertThat(waiter.active()).isTrue();
    }

    @Test
    void nobodyBecomesOwnerThroughTheTeam() {
        assertThatThrownBy(() -> Membership.teamMember(carlos, sakura, Role.OWNER, PIN))
                .isInstanceOf(InvalidValueException.class);
        Membership waiter = Membership.teamMember(carlos, sakura, Role.STAFF, PIN);
        assertThatThrownBy(() -> waiter.changeRole(Role.OWNER)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void teamMemberNeedsAPin() {
        assertThatThrownBy(() -> new Membership(MembershipId.newId(), carlos, sakura, Role.KITCHEN, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesReturnANewMembershipAndKeepTheRest() {
        Membership waiter = Membership.teamMember(carlos, sakura, Role.STAFF, PIN);
        PasswordHash newPin = new PasswordHash("hash:7391");

        Membership kitchen = waiter.changeRole(Role.KITCHEN).changePin(newPin);

        assertThat(kitchen.id()).isEqualTo(waiter.id());
        assertThat(kitchen.role()).isEqualTo(Role.KITCHEN);
        assertThat(kitchen.pinHash()).isEqualTo(newPin);
        assertThat(waiter.role()).isEqualTo(Role.STAFF); // o original não muda
    }

    @Test
    void deactivateKeepsTheRecord() {
        Membership waiter = Membership.teamMember(carlos, sakura, Role.STAFF, PIN);

        Membership gone = waiter.deactivate();

        assertThat(gone.active()).isFalse();
        assertThat(gone.id()).isEqualTo(waiter.id());
    }

    @Test
    void reactivateBringsBackTheSameRoleAndPin() {
        Membership waiter = Membership.teamMember(carlos, sakura, Role.STAFF, PIN).deactivate();

        Membership back = waiter.reactivate();

        assertThat(back.active()).isTrue();
        assertThat(back.id()).isEqualTo(waiter.id());
        assertThat(back.role()).isEqualTo(Role.STAFF);
        assertThat(back.pinHash()).isEqualTo(PIN);
    }
}