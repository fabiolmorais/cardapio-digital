package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.DeclineCallUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class DeclineCallService implements DeclineCallUseCase {

    private final CallPort calls;
    private final EventPublisherPort events;
    private final Clock clock;

    public DeclineCallService(CallPort calls, EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StaffCall decline(EstablishmentId establishmentId, CallId callId) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        Instant now = clock.instant();
        Call declined = call.decline(now);
        if (!calls.update(declined, CallStatus.PENDING)) {
            throw new CallStateException("Outra pessoa já respondeu este pedido");
        }
        events.publish(CallChanged.of(declined, now));
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
