package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.CancelCallUseCase;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class CancelCallService implements CancelCallUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final CallPort calls;
    private final EventPublisherPort events;
    private final Clock clock;

    public CancelCallService(DiningTablePort tables, TableSessionPort sessions, CallPort calls,
                             EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.calls = calls;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cancel(String tableToken, String sessionToken, CallId callId) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, now);
        // Qualquer celular da mesa pode cancelar o chamado da mesa; o de outra mesa, não (404).
        Call call = calls.find(visit.table().establishmentId(), callId)
                .filter(c -> c.tableId().equals(visit.table().id()))
                .orElseThrow(CallNotFoundException::new);
        Call canceled = call.cancel(now);
        if (!calls.update(canceled, call.status())) {
            throw new CallStateException("Este chamado acabou de mudar. Atualize a página.");
        }
        events.publish(CallChanged.of(canceled, now));
    }
}
