package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.AcceptCallUseCase;
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
import br.com.cardapio.domain.call.model.CallType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class AcceptCallService implements AcceptCallUseCase {

    private final CallPort calls;
    private final TableSessionPort sessions;
    private final EventPublisherPort events;
    private final Clock clock;

    public AcceptCallService(CallPort calls, TableSessionPort sessions, EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Dois garçons tocam em "Aceitar" quase juntos: os dois leem o chamado pendente e os dois
     * passam pelo accept() do domínio. Quem decide é o UPDATE condicional ("só se ainda estiver
     * pendente"): o banco aplica um, e o outro recebe 409.
     *
     * No pedido de liberação, "aceitar" é liberar: o chamado termina na hora, e as sessões da mesa
     * ficam com a presença confirmada pelo garçom.
     */
    @Override
    @Transactional
    public StaffCall accept(EstablishmentId establishmentId, CallId callId, UserId by) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        Instant now = clock.instant();
        Call changed = call.type() == CallType.UNLOCK ? call.release(by, now) : call.accept(by, now);
        if (!calls.update(changed, CallStatus.PENDING)) {
            throw new CallStateException("Outra pessoa já aceitou este chamado");
        }
        if (changed.type() == CallType.UNLOCK) {
            sessions.unlockTable(establishmentId, changed.tableId(), by, now);
        }
        events.publish(CallChanged.of(changed, now));
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
