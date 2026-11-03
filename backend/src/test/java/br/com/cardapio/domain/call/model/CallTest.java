package br.com.cardapio.domain.call.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private final UserId carlos = UserId.newId();

    private static Call open(CallType type, PaymentMethod method) {
        return Call.open(EstablishmentId.newId(), DiningTableId.newId(), TableSessionId.newId(), type, method, NOW);
    }

    @Test
    void aNewCallIsPendingAndOnlyTheBillHasAPaymentMethod() {
        Call waiter = open(CallType.WAITER, PaymentMethod.PIX);
        Call bill = open(CallType.BILL, PaymentMethod.CARD);

        assertThat(waiter.status()).isEqualTo(CallStatus.PENDING);
        assertThat(waiter.paymentMethod()).isNull();
        assertThat(bill.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(bill.createdAt()).isEqualTo(NOW);
    }

    @Test
    void theBillNeedsAPaymentMethod() {
        assertThatThrownBy(() -> open(CallType.BILL, null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void acceptingRecordsWhoAndWhenThenFinishingClosesIt() {
        Call accepted = open(CallType.WAITER, null).accept(carlos, NOW.plusSeconds(30));
        Call done = accepted.finish(NOW.plusSeconds(90));

        assertThat(accepted.status()).isEqualTo(CallStatus.IN_PROGRESS);
        assertThat(accepted.acceptedBy()).isEqualTo(carlos);
        assertThat(accepted.acceptedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(done.status()).isEqualTo(CallStatus.DONE);
        assertThat(done.doneAt()).isEqualTo(NOW.plusSeconds(90));
        assertThat(done.isOpen()).isFalse();
    }

    @Test
    void onlyOnePersonAccepts() {
        Call accepted = open(CallType.WAITER, null).accept(carlos, NOW);

        assertThatThrownBy(() -> accepted.accept(UserId.newId(), NOW))
                .isInstanceOf(CallStateException.class).hasMessage("Outra pessoa já aceitou este chamado");
    }

    @Test
    void finishingNeedsSomeoneToHaveAccepted() {
        assertThatThrownBy(() -> open(CallType.WAITER, null).finish(NOW))
                .isInstanceOf(CallStateException.class).hasMessage("Aceite o chamado antes de concluir");
    }

    @Test
    void theTableCancelsWhileOpenButNotAfter() {
        Call canceled = open(CallType.WAITER, null).accept(carlos, NOW).cancel(NOW.plusSeconds(5));

        assertThat(canceled.status()).isEqualTo(CallStatus.CANCELED);
        assertThat(canceled.canceledAt()).isEqualTo(NOW.plusSeconds(5));
        assertThatThrownBy(() -> canceled.cancel(NOW)).isInstanceOf(CallStateException.class);
        assertThatThrownBy(() -> canceled.accept(carlos, NOW)).isInstanceOf(CallStateException.class);
        assertThatThrownBy(() -> canceled.finish(NOW)).isInstanceOf(CallStateException.class);
    }

    @Test
    void theBillChangesItsPaymentMethodWhileOpen() {
        Call bill = open(CallType.BILL, PaymentMethod.CARD).withPaymentMethod(PaymentMethod.CASH);

        assertThat(bill.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        Call done = bill.accept(carlos, NOW).finish(NOW);
        assertThatThrownBy(() -> done.withPaymentMethod(PaymentMethod.PIX)).isInstanceOf(CallStateException.class);
    }

    @Test
    void theUnlockRequestIsReleasedOrDeclinedInOneTouch() {
        Call request = open(CallType.UNLOCK, PaymentMethod.PIX);

        Call released = request.release(carlos, NOW.plusSeconds(20));
        Call declined = request.decline(NOW.plusSeconds(20));

        assertThat(request.paymentMethod()).isNull();
        assertThat(released.status()).isEqualTo(CallStatus.DONE);
        assertThat(released.acceptedBy()).isEqualTo(carlos);
        assertThat(released.doneAt()).isEqualTo(NOW.plusSeconds(20));
        assertThat(declined.status()).isEqualTo(CallStatus.CANCELED);
        assertThatThrownBy(() -> request.accept(carlos, NOW)).isInstanceOf(CallStateException.class);
        assertThatThrownBy(() -> released.decline(NOW)).isInstanceOf(CallStateException.class);
        assertThatThrownBy(() -> open(CallType.WAITER, null).release(carlos, NOW))
                .isInstanceOf(CallStateException.class);
    }

    @Test
    void anOpenCallEndsWithTheTab() {
        Call pending = open(CallType.BILL, PaymentMethod.PIX).closeWithTab(NOW.plusSeconds(600));
        Call accepted = open(CallType.WAITER, null).accept(carlos, NOW).closeWithTab(NOW.plusSeconds(600));

        assertThat(pending.status()).isEqualTo(CallStatus.DONE);
        assertThat(pending.doneAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(accepted.status()).isEqualTo(CallStatus.DONE);
        assertThat(accepted.acceptedBy()).isEqualTo(carlos);
        assertThatThrownBy(() -> pending.closeWithTab(NOW)).isInstanceOf(CallStateException.class);
    }
}
