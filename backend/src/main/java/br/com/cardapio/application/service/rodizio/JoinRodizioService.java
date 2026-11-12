package br.com.cardapio.application.service.rodizio;

import br.com.cardapio.application.port.in.rodizio.JoinRodizioUseCase;
import br.com.cardapio.application.port.out.call.CallPort;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.out.rodizio.RodizioPhonePort;
import br.com.cardapio.application.port.out.rodizio.TabRodizioPort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.call.event.CallChanged;
import br.com.cardapio.domain.call.model.Call;
import br.com.cardapio.domain.call.model.CallType;
import br.com.cardapio.domain.presence.exception.PresenceRequiredException;
import br.com.cardapio.domain.rodizio.exception.NoRodizioOnTableException;
import br.com.cardapio.domain.rodizio.model.RodizioParticipation;
import br.com.cardapio.domain.rodizio.model.TabRodizio;
import br.com.cardapio.domain.tablesession.exception.SessionBlockedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class JoinRodizioService implements JoinRodizioUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final TabPort tabs;
    private final TabRodizioPort tabRodizios;
    private final RodizioPhonePort phones;
    private final CallPort calls;
    private final EventPublisherPort events;
    private final Clock clock;

    public JoinRodizioService(DiningTablePort tables, TableSessionPort sessions, TabPort tabs,
                              TabRodizioPort tabRodizios, RodizioPhonePort phones, CallPort calls,
                              EventPublisherPort events, Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.tabs = tabs;
        this.tabRodizios = tabRodizios;
        this.phones = phones;
        this.calls = calls;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Numa transação: o celular fica "pedindo para entrar" e a mesa chama o garçom com o "Quero o
     * rodízio" (um só aberto por mesa: se outro celular já chamou, este entra no mesmo chamado).
     * As travas de sempre: celular bloqueado e presença no salão.
     */
    @Override
    @Transactional
    public RodizioParticipation join(String tableToken, String sessionToken) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, tableToken, sessionToken, now);
        if (visit.session().isBlocked()) {
            throw new SessionBlockedException();
        }
        if (!visit.session().isPresent()) {
            throw new PresenceRequiredException();
        }
        var establishmentId = visit.table().establishmentId();
        var tab = tabs.findOpen(establishmentId, visit.table().id()).orElseThrow(NoRodizioOnTableException::new);
        TabRodizio rodizio = tabRodizios.findByTab(establishmentId, tab.id())
                .orElseThrow(NoRodizioOnTableException::new);
        if (phones.find(establishmentId, rodizio.id(), visit.session().id()) == RodizioParticipation.JOINED) {
            return RodizioParticipation.JOINED;
        }
        phones.request(establishmentId, rodizio.id(), visit.session().id(), now);
        Call call = Call.open(establishmentId, visit.table().id(), visit.session().id(), CallType.RODIZIO, null, now);
        if (calls.insertIfNoneOpen(call)) {
            events.publish(CallChanged.of(call, now));
        }
        return RodizioParticipation.REQUESTED;
    }
}
