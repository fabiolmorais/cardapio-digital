package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.RequestCallCommand;
import br.com.cardapio.application.port.in.call.RequestedCall;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.model.CallStatus;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.exception.TableSessionRequiredException;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O lado da mesa: chamar, pedir a conta, ver e cancelar, sempre com a sessão do celular. */
class TableCallServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private static final String QR_04 = "qr-da-mesa-04-00000000000";
    private static final String QR_05 = "qr-da-mesa-05-00000000000";

    private final Map<TokenHash, TableSession> sessionsByHash = new HashMap<>();
    private final TableSessionPort sessions = new TableSessionPort() {
        @Override
        public void save(TableSession session) {
            sessionsByHash.put(session.tokenHash(), session);
        }

        @Override
        public Optional<TableSession> findByTokenHash(TokenHash tokenHash) {
            return Optional.ofNullable(sessionsByHash.get(tokenHash));
        }
    };
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryCalls calls = new InMemoryCalls();
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private DiningTable mesa04;

    @BeforeEach
    void tablesWithASessionOn04() {
        mesa04 = tables.given(sakura, "04", QR_04);
        DiningTable mesa05 = tables.given(sakura, "05", QR_05);
        sessions.save(TableSession.open(sakura, mesa04.id(), TokenHash.of("celular-04"), NOW));
        sessions.save(TableSession.open(sakura, mesa05.id(), TokenHash.of("celular-05"), NOW));
    }

    private RequestedCall request(String session, CallType type, PaymentMethod method) {
        return new RequestCallService(tables, sessions, calls, clock)
                .request(new RequestCallCommand(QR_04, session, type, method));
    }

    @Test
    void callingTheWaiterOpensAPendingCall() {
        RequestedCall result = request("celular-04", CallType.WAITER, null);

        assertThat(result.created()).isTrue();
        assertThat(result.call().status()).isEqualTo(CallStatus.PENDING);
        assertThat(calls.saved).hasSize(1);
    }

    @Test
    void tappingAgainReturnsTheSameCallInsteadOfADuplicate() {
        var first = request("celular-04", CallType.WAITER, null);
        var again = request("celular-04", CallType.WAITER, null);

        assertThat(again.created()).isFalse();
        assertThat(again.call().id()).isEqualTo(first.call().id());
        assertThat(calls.saved).hasSize(1);
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
    }

    @Test
    void theTableSeesItsOpenCallsAndCancelsThem() {
        var call = request("celular-04", CallType.WAITER, null).call();

        assertThat(new ListTableCallsService(tables, sessions, calls, clock).list(QR_04, "celular-04"))
                .extracting(c -> c.id()).containsExactly(call.id());

        new CancelCallService(tables, sessions, calls, clock).cancel(QR_04, "celular-04", call.id());

        assertThat(calls.saved.get(call.id()).status()).isEqualTo(CallStatus.CANCELED);
        assertThat(new ListTableCallsService(tables, sessions, calls, clock).list(QR_04, "celular-04")).isEmpty();
    }

    @Test
    void anotherTableCannotCancelIt() {
        var call = request("celular-04", CallType.WAITER, null).call();

        assertThatThrownBy(() -> new CancelCallService(tables, sessions, calls, clock)
                .cancel(QR_05, "celular-05", call.id()))
                .isInstanceOf(CallNotFoundException.class);
        assertThat(calls.saved.get(call.id()).status()).isEqualTo(CallStatus.PENDING);
    }
}
