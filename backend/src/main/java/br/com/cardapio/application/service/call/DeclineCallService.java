package br.com.cardapio.application.service.call;

import br.com.cardapio.application.port.in.call.DeclineCallUseCase;
import br.com.cardapio.application.port.in.call.StaffCall;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.exception.CallNotFoundException;
import br.com.cardapio.domain.call.exception.CallStateException;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallId;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.rodizio.event.RodizioChanged;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class DeclineCallService implements DeclineCallUseCase {

    private final CallPort calls;
    private final TabPort tabs;
    private final TabRodizioPort tabRodizios;
    private final RodizioPhonePort phones;
    private final EventPublisherPort events;
    private final Clock clock;

    public DeclineCallService(CallPort calls, TabPort tabs, TabRodizioPort tabRodizios, RodizioPhonePort phones,
                              EventPublisherPort events, Clock clock) {
        this.calls = calls;
        this.tabs = tabs;
        this.tabRodizios = tabRodizios;
        this.phones = phones;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StaffCall decline(EstablishmentId establishmentId, CallId callId) {
        Call call = calls.find(establishmentId, callId).orElseThrow(CallNotFoundException::new);
        Instant now = clock.instant();
        Call declined = call.decline(now);
        if (!calls.update(declined, call.status())) {
            throw new CallStateException("Outra pessoa já respondeu este pedido");
        }
        events.publish(CallChanged.of(declined, now));
        // "Quero o rodízio" recusado numa mesa que já está no rodízio: quem pediu para entrar volta
        // a "não está", e o celular dele mostra de novo a pergunta (pode pedir outra vez).
        if (call.type() == CallType.RODIZIO) {
            tabs.findOpen(establishmentId, call.tableId())
                    .flatMap(tab -> tabRodizios.findByTab(establishmentId, tab.id()))
                    .ifPresent(rodizio -> {
                        phones.cancelRequests(establishmentId, rodizio.id());
                        events.publish(new RodizioChanged(establishmentId, call.tableId(), rodizio.tabId(), now));
                    });
        }
        return StaffCall.from(calls.findView(establishmentId, callId).orElseThrow());
    }
}
