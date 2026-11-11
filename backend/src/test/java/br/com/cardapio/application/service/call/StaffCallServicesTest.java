package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.exception.WrongReleaseCodeException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import br.com.cardapio.support.InMemoryTableSessions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O lado da equipe: a fila, aceitar, concluir, e liberar ou recusar uma mesa. */
class StaffCallServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");

    private final InMemoryCalls calls = new InMemoryCalls();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    /** O que os serviços publicaram para o tempo real. */
    private final List<DomainEvent> published = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final UserId carlos = UserId.newId();

    private AcceptCallService accept() {
        return new AcceptCallService(calls, published::add, clock);
    }

    private FinishCallService finish() {
        return new FinishCallService(calls, published::add, clock);
    }

    private Call given(Instant at) {
        Call call = Call.open(sakura, DiningTableId.newId(), TableSessionId.newId(), CallType.WAITER, null, at);
        calls.insertIfNoneOpen(call);
        return call;
    }

    @Test
    void theQueueHasOnlyOpenCallsOldestFirst() {
        Call later = given(NOW.plusSeconds(60));
        Call first = given(NOW);
        Call done = given(NOW.minusSeconds(60));
        calls.update(done.accept(carlos, NOW).finish(NOW), CallStatus.PENDING);

        var queue = new ListCallQueueService(calls).list(sakura, new PageQuery(0, 50));

        assertThat(queue.content()).extracting(StaffCall::id).containsExactly(first.id(), later.id());
    }

    @Test
    void acceptingShowsWhoIsGoing() {
        Call call = given(NOW);
        calls.names.put(carlos, "Carlos Souza");

        StaffCall accepted = accept().accept(sakura, call.id(), carlos);

        assertThat(accepted.status()).isEqualTo(CallStatus.IN_PROGRESS);
        assertThat(accepted.acceptedByName()).isEqualTo("Carlos Souza");
        assertThat(accepted.acceptedAt()).isEqualTo(NOW);
        assertThat(published).singleElement().isInstanceOfSatisfying(CallChanged.class,
                event -> assertThat(event.status()).isEqualTo(CallStatus.IN_PROGRESS));
    }

    @Test
    void theSecondToAcceptGetsAConflict() {
        Call call = given(NOW);
        accept().accept(sakura, call.id(), carlos);

        assertThatThrownBy(() -> accept().accept(sakura, call.id(), UserId.newId()))
                .isInstanceOf(CallStateException.class).hasMessage("Outra pessoa já aceitou este chamado");
        assertThat(calls.saved.get(call.id()).acceptedBy()).isEqualTo(carlos);
        assertThat(published).hasSize(1); // só o aceite que valeu
    }

    @Test
    void finishingNeedsTheAcceptFirst() {
        Call call = given(NOW);

        assertThatThrownBy(() -> finish().finish(sakura, call.id()))
                .isInstanceOf(CallStateException.class);
        assertThat(published).isEmpty();

        accept().accept(sakura, call.id(), carlos);
        assertThat(finish().finish(sakura, call.id()).status())
                .isEqualTo(CallStatus.DONE);
        assertThat(published.getLast()).isInstanceOfSatisfying(CallChanged.class,
                event -> assertThat(event.status()).isEqualTo(CallStatus.DONE));
    }

    @Test
    void anotherRestaurantDoesNotFindTheCall() {
        Call call = given(NOW);
        EstablishmentId other = EstablishmentId.newId();

        assertThatThrownBy(() -> accept().accept(other, call.id(), carlos))
                .isInstanceOf(CallNotFoundException.class);
        assertThatThrownBy(() -> finish().finish(other, call.id()))
                .isInstanceOf(CallNotFoundException.class);
        assertThat(new ListCallQueueService(calls).list(other, new PageQuery(0, 50)).content()).isEmpty();
    }

    /** O código que aparece no celular de quem pediu a liberação. */
    private static final String CODE = "4719";

    private ReleaseCallService release() {
        return new ReleaseCallService(calls, sessions, published::add, clock);
    }

    /** Um pedido de liberação de um celular que não provou a presença, e um vizinho de mesa na mesma situação. */
    private Call unlockRequest(DiningTableId mesa) {
        TableSession phone = sessions.given(sakura, mesa, "celular-de-fora", NOW);
        sessions.given(sakura, mesa, "celular-do-amigo", NOW);
        Call call = Call.openUnlock(sakura, mesa, phone.id(), CODE, NOW);
        calls.insertIfNoneOpen(call);
        return call;
    }

    @Test
    void releasingWithTheCodeUnlocksOnlyThePhoneThatAskedAndClosesTheRequest() {
        DiningTableId mesa04 = DiningTableId.newId();
        Call call = unlockRequest(mesa04);

        StaffCall released = release().release(sakura, call.id(), carlos, CODE);

        assertThat(released.status()).isEqualTo(CallStatus.DONE);
        assertThat(sessions.byToken("celular-de-fora").presenceMethod()).isEqualTo(PresenceMethod.STAFF);
        assertThat(sessions.byToken("celular-de-fora").unlockedBy()).isEqualTo(carlos);
        // Quem abriu o QR na mesma mesa sem pedir (de casa, por exemplo) continua travado.
        assertThat(sessions.byToken("celular-do-amigo").isPresent()).isFalse();
        assertThat(published).singleElement().isInstanceOfSatisfying(CallChanged.class,
                event -> assertThat(event.status()).isEqualTo(CallStatus.DONE));
    }

    @Test
    void aWrongCodeReleasesNothingAndTheRequestStaysOpen() {
        Call call = unlockRequest(DiningTableId.newId());

        assertThatThrownBy(() -> release().release(sakura, call.id(), carlos, "0000"))
                .isInstanceOf(WrongReleaseCodeException.class);

        assertThat(sessions.byToken("celular-de-fora").isPresent()).isFalse();
        assertThat(calls.saved.get(call.id()).status()).isEqualTo(CallStatus.PENDING);
        assertThat(published).isEmpty();
    }

    @Test
    void anUnlockRequestIsNotAcceptedWithoutTheCode() {
        Call call = unlockRequest(DiningTableId.newId());

        assertThatThrownBy(() -> accept().accept(sakura, call.id(), carlos)).isInstanceOf(CallStateException.class);
        assertThat(sessions.byToken("celular-de-fora").isPresent()).isFalse();
    }

    @Test
    void anotherRestaurantCannotRelease() {
        Call call = unlockRequest(DiningTableId.newId());

        assertThatThrownBy(() -> release().release(EstablishmentId.newId(), call.id(), carlos, CODE))
                .isInstanceOf(CallNotFoundException.class);
    }

    @Test
    void decliningKeepsTheTableLocked() {
        Call call = unlockRequest(DiningTableId.newId());

        StaffCall declined = new DeclineCallService(calls, published::add, clock).decline(sakura, call.id());

        assertThat(declined.status()).isEqualTo(CallStatus.CANCELED);
        assertThat(sessions.byToken("celular-de-fora").isPresent()).isFalse();
        assertThat(published).hasSize(1);
    }

    @Test
    void onlyAnUnlockRequestCanBeDeclined() {
        Call waiter = given(NOW);

        assertThatThrownBy(() -> new DeclineCallService(calls, published::add, clock).decline(sakura, waiter.id()))
                .isInstanceOf(CallStateException.class);
    }

    @Test
    void blockingThePhoneCancelsItsOpenCalls() {
        DiningTableId mesa = DiningTableId.newId();
        TableSession prankster = sessions.given(sakura, mesa, "trote", NOW);
        TableSession neighbour = sessions.given(sakura, mesa, "vizinho", NOW);
        Call waiter = Call.open(sakura, mesa, prankster.id(), CallType.WAITER, null, NOW);
        Call bill = Call.open(sakura, mesa, prankster.id(), CallType.BILL, PaymentMethod.PIX, NOW);
        Call theirs = Call.open(sakura, DiningTableId.newId(), neighbour.id(), CallType.WAITER, null, NOW);
        for (Call c : new Call[]{waiter, bill, theirs}) {
            calls.insertIfNoneOpen(c);
        }

        new BlockCallerService(calls, sessions, published::add, clock).block(sakura, waiter.id(), carlos);

        assertThat(sessions.byToken("trote").isBlocked()).isTrue();
        assertThat(sessions.byToken("vizinho").isBlocked()).isFalse();
        assertThat(calls.saved.get(waiter.id()).status()).isEqualTo(CallStatus.CANCELED);
        assertThat(calls.saved.get(bill.id()).status()).isEqualTo(CallStatus.CANCELED);
        assertThat(calls.saved.get(theirs.id()).status()).isEqualTo(CallStatus.PENDING);
        assertThat(published).hasSize(2);
    }

    @Test
    void anotherRestaurantCannotBlock() {
        Call call = given(NOW);

        assertThatThrownBy(() -> new BlockCallerService(calls, sessions, published::add, clock)
                .block(EstablishmentId.newId(), call.id(), carlos))
                .isInstanceOf(CallNotFoundException.class);
    }
}
