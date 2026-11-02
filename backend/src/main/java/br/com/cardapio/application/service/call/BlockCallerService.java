package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.BlockCallerUseCase;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class BlockCallerService implements BlockCallerUseCase {

    private final CallPort calls;
    private final TableSessionPort sessions;
    private final EventPublisherPort events;
    private final Clock clock;

    public BlockCallerService(CallPort calls, TableSessionPort sessions, EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Quem bloqueia escolhe o CHAMADO (é o que ele vê na fila); o celular é o da sessão que o abriu.
     * Bloquear de novo não muda nada (e não dá erro).
     */
    @Override
    @Transactional
    public void block(EstablishmentId establishmentId, CallId callId, UserId by) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        Instant now = clock.instant();
        sessions.block(establishmentId, call.sessionId(), by, now);
        for (Call open : calls.findOpenBySession(establishmentId, call.sessionId())) {
            Call canceled = open.cancel(now);
            // Se mudou no meio do caminho (outro garçom concluiu), fica como está.
            if (calls.update(canceled, open.status())) {
                events.publish(CallChanged.of(canceled, now));
            }
        }
    }
}
