package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.application.port.in.tablesession.ListBlockedPhonesUseCase;
import br.com.cardapio.application.port.in.tablesession.UnblockPhoneUseCase;
import br.com.cardapio.application.port.out.realtime.EventPublisherPort;
import br.com.cardapio.application.port.in.tablesession.BlockedPhone;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.event.PhoneBlockChanged;
import br.com.cardapio.domain.tablesession.exception.TableSessionNotFoundException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Os celulares bloqueados pela equipe: a lista e o "Desbloquear". Uma classe só atende aos dois
 * casos de uso: eles dividem as mesmas dependências.
 */
public class BlockedPhonesService implements ListBlockedPhonesUseCase, UnblockPhoneUseCase {

    private final TableSessionPort sessions;
    private final EventPublisherPort events;
    private final Clock clock;

    public BlockedPhonesService(TableSessionPort sessions, EventPublisherPort events, Clock clock) {
        this.sessions = sessions;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<BlockedPhone> list(EstablishmentId establishmentId, PageQuery page) {
        var views = sessions.findBlocked(establishmentId, clock.instant(), page);
        return new ResultPage<>(views.content().stream().map(BlockedPhone::from).toList(), views.page(),
                views.size(), views.totalElements());
    }

    /**
     * Desbloqueia e avisa a mesa e a equipe. Desbloquear quem já não está bloqueado não muda nada
     * (e não dá erro); celular de outro restaurante, 404.
     */
    @Override
    @Transactional
    public void unblock(EstablishmentId establishmentId, TableSessionId sessionId) {
        DiningTableId tableId = sessions.findTable(establishmentId, sessionId)
                .orElseThrow(TableSessionNotFoundException::new);
        if (sessions.unblock(establishmentId, sessionId)) {
            Instant now = clock.instant();
            events.publish(new PhoneBlockChanged(establishmentId, tableId, sessionId, false, now));
        }
    }
}
