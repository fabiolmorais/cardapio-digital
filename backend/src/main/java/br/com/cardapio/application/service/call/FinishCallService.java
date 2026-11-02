package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.FinishCallUseCase;
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

public class FinishCallService implements FinishCallUseCase {

    private final CallPort calls;
    private final EventPublisherPort events;
    private final Clock clock;

    public FinishCallService(CallPort calls, EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StaffCall finish(EstablishmentId establishmentId, CallId callId) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        Instant now = clock.instant();
        Call done = call.finish(now);
        if (!calls.update(done, CallStatus.IN_PROGRESS)) {
            throw new CallStateException("Este chamado acabou de mudar. Atualize a fila.");
        }
        events.publish(CallChanged.of(done, now));
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
