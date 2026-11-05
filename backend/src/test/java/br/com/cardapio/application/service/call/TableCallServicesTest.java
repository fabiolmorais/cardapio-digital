package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.RequestCallCommand;
import br.com.cardapio.application.port.in.call.RequestedCall;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.exception.TooManyCallsException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.plan.exception.ServiceSuspendedException;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.shared.event.DomainEvent;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import br.com.cardapio.domain.tablesession.exception.TableSessionRequiredException;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.support.InMemorySubscriptions;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O lado da mesa: chamar, pedir a conta, ver e cancelar, sempre com a sessão do celular. */
class TableCallServicesTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";
    private static final String QR_05 = "qr-da-mesa-05-00000000000";

    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryCalls calls = new InMemoryCalls();
    /** O que os serviços publicaram para o tempo real. */
    private final List<DomainEvent> published = new ArrayList<>();
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private DiningTable mesa04;

    @BeforeEach
    void tablesWithASessionOn04() {
        mesa04 = tables.given(sakura, "04", QR_04);
        DiningTable mesa05 = tables.given(sakura, "05", QR_05);
        sessions.givenPresent(sakura, mesa04.id(), "celular-04", NOW);
        sessions.givenPresent(sakura, mesa05.id(), "celular-05", NOW);
    }

    private CancelCallService cancel() {
        return new CancelCallService(tables, sessions, calls, published::add, clock);
    }

    private RequestedCall request(String session, CallType type, PaymentMethod method) {
        return new RequestCallService(tables, sessions, calls, subscriptions, published::add, clock)
                .request(new RequestCallCommand(QR_04, session, type, method));
    }

    @Test
    void callingTheWaiterOpensAPendingCall() {
        RequestedCall result = request("celular-04", CallType.WAITER, null);

        assertThat(result.created()).isTrue();
        assertThat(result.call().status()).isEqualTo(CallStatus.PENDING);
        assertThat(calls.saved).hasSize(1);
        // E a equipe fica sabendo: um aviso, com a mesa e o status.
        assertThat(published).singleElement().isInstanceOfSatisfying(CallChanged.class, event -> {
            assertThat(event.tableId()).isEqualTo(mesa04.id());
            assertThat(event.status()).isEqualTo(CallStatus.PENDING);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void tappingAgainReturnsTheSameCallInsteadOfADuplicate() {
        var first = request("celular-04", CallType.WAITER, null);
        var again = request("celular-04", CallType.WAITER, null);

        assertThat(again.created()).isFalse();
        assertThat(again.call().id()).isEqualTo(first.call().id());
        assertThat(calls.saved).hasSize(1);
        assertThat(published).hasSize(1); // o segundo toque não muda nada, então não avisa ninguém
        // A conta é outro tipo: pode estar aberta ao mesmo tempo.
        assertThat(request("celular-04", CallType.BILL, PaymentMethod.PIX).created()).isTrue();
    }

    @Test
    void askingForTheBillAgainChangesThePaymentMethod() {
        request("celular-04", CallType.BILL, PaymentMethod.CARD);

        var changed = request("celular-04", CallType.BILL, PaymentMethod.CASH);

        assertThat(changed.created()).isFalse();
        assertThat(changed.call().paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(calls.saved.values()).singleElement()
                .satisfies(c -> assertThat(c.paymentMethod()).isEqualTo(PaymentMethod.CASH));
        assertThat(published).hasSize(2); // pedir e trocar a forma de pagamento
    }

    @Test
    void withoutAValidSessionOnThisTableNothingIsCalled() {
        for (String session : new String[]{null, "", "inventado", "celular-05"}) {
            assertThatThrownBy(() -> request(session, CallType.WAITER, null))
                    .as(String.valueOf(session)).isInstanceOf(TableSessionRequiredException.class);
        }
        clock = Clock.offset(clock, TableSession.IDLE_LIMIT);
        assertThatThrownBy(() -> request("celular-04", CallType.WAITER, null))
                .isInstanceOf(TableSessionRequiredException.class);
        assertThat(calls.saved).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void theTableSeesItsOpenCallsAndCancelsThem() {
        var call = request("celular-04", CallType.WAITER, null).call();

        assertThat(new ListTableCallsService(tables, sessions, calls, clock).list(QR_04, "celular-04"))
                .extracting(c -> c.id()).containsExactly(call.id());

        cancel().cancel(QR_04, "celular-04", call.id());

        assertThat(calls.saved.get(call.id()).status()).isEqualTo(CallStatus.CANCELED);
        assertThat(published.getLast()).isInstanceOfSatisfying(CallChanged.class,
                event -> assertThat(event.status()).isEqualTo(CallStatus.CANCELED));
        assertThat(new ListTableCallsService(tables, sessions, calls, clock).list(QR_04, "celular-04")).isEmpty();
    }

    @Test
    void anotherTableCannotCancelIt() {
        var call = request("celular-04", CallType.WAITER, null).call();

        assertThatThrownBy(() -> cancel()
                .cancel(QR_05, "celular-05", call.id()))
                .isInstanceOf(CallNotFoundException.class);
        assertThat(calls.saved.get(call.id()).status()).isEqualTo(CallStatus.PENDING);
    }

    @Test
    void withoutPresenceTheButtonsAreLockedButTheUnlockRequestGoesThrough() {
        sessions.given(sakura, mesa04.id(), "celular-de-fora", NOW);

        for (CallType type : new CallType[]{CallType.WAITER, CallType.BILL}) {
            assertThatThrownBy(() -> request("celular-de-fora", type, PaymentMethod.PIX))
                    .as(type.name()).isInstanceOf(PresenceRequiredException.class);
        }
        RequestedCall unlock = request("celular-de-fora", CallType.UNLOCK, null);

        assertThat(unlock.created()).isTrue();
        assertThat(unlock.call().type()).isEqualTo(CallType.UNLOCK);
        assertThat(calls.saved).hasSize(1);
    }

    @Test
    void aPhoneAlreadyInTheRoomDoesNotAskForUnlock() {
        assertThatThrownBy(() -> request("celular-04", CallType.UNLOCK, null))
                .isInstanceOf(CallStateException.class).hasMessage("Sua mesa já está liberada.");
    }

    @Test
    void openingAndCancelingWithoutStopHitsTheLimit() {
        for (int i = 0; i < Call.MAX_NEW_PER_MINUTE; i++) {
            var call = request("celular-04", CallType.WAITER, null).call();
            cancel().cancel(QR_04, "celular-04", call.id());
        }

        assertThatThrownBy(() -> request("celular-04", CallType.WAITER, null))
                .isInstanceOf(TooManyCallsException.class);
        // Um minuto depois, pode de novo.
        clock = Clock.offset(clock, Duration.ofMinutes(1));
        assertThat(request("celular-04", CallType.WAITER, null).created()).isTrue();
    }

    @Test
    void tappingAnOpenCallAgainDoesNotCount() {
        for (int i = 0; i < 10; i++) {
            request("celular-04", CallType.WAITER, null);
        }

        assertThat(calls.saved).hasSize(1);
    }

    @Test
    void aBlockedPhoneCallsNothingNotEvenTheUnlock() {
        sessions.save(sessions.byToken("celular-04").block(UserId.newId(), NOW));
        sessions.given(sakura, mesa04.id(), "bloqueado-sem-presenca", NOW);
        sessions.save(sessions.byToken("bloqueado-sem-presenca").block(UserId.newId(), NOW));

        assertThatThrownBy(() -> request("celular-04", CallType.WAITER, null))
                .isInstanceOf(SessionBlockedException.class);
        assertThatThrownBy(() -> request("bloqueado-sem-presenca", CallType.UNLOCK, null))
                .isInstanceOf(SessionBlockedException.class);
    }

    @Test
    void aSuspendedSubscriptionStopsTheCallsAndNothingIsSaved() {
        subscriptions.suspended();

        assertThatThrownBy(() -> request("celular-04", CallType.WAITER, null))
                .isInstanceOf(ServiceSuspendedException.class)
                .hasMessage("O atendimento pelo celular está pausado neste restaurante. Chame um garçom.");
        assertThat(calls.saved).isEmpty();
        assertThat(published).isEmpty();
    }
}
