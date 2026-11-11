package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.ReleaseCallUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class ReleaseCallService implements ReleaseCallUseCase {

    private final CallPort calls;
    private final TableSessionPort sessions;
    private final EventPublisherPort events;
    private final Clock clock;

    public ReleaseCallService(CallPort calls, TableSessionPort sessions, EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * O código confere: o chamado termina e a sessão de quem pediu fica com a presença confirmada
     * pelo garçom. As outras sessões da mesa não mudam. Código errado: nada muda (422).
     */
    @Override
    @Transactional
    public StaffCall release(EstablishmentId establishmentId, CallId callId, UserId by, String code) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        Instant now = clock.instant();
        Call released = call.release(by, code, now);
        if (!calls.update(released, CallStatus.PENDING)) {
            throw new CallStateException("Outra pessoa já respondeu este pedido");
        }
        sessions.unlockSession(establishmentId, released.sessionId(), by, now);
        events.publish(CallChanged.of(released, now));
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
