package br.com.cardapio.application.service.presence;

import br.com.cardapio.application.port.in.presence.CheckPresenceCommand;
import br.com.cardapio.application.port.in.presence.CheckPresenceUseCase;
import br.com.cardapio.application.port.in.presence.PresenceStatus;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.tablesession.TableVisits;
import br.com.cardapio.domain.presence.model.PresenceRule;
import br.com.cardapio.domain.tablesession.model.TableSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class CheckPresenceService implements CheckPresenceUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final PresenceRulePort rules;
    private final Clock clock;

    public CheckPresenceService(DiningTablePort tables, TableSessionPort sessions, PresenceRulePort rules,
                                Clock clock) {
        this.tables = tables;
        this.sessions = sessions;
        this.rules = rules;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PresenceStatus check(CheckPresenceCommand command) {
        Instant now = clock.instant();
        var visit = TableVisits.require(tables, sessions, command.tableToken(), command.sessionToken(), now);
        PresenceRule rule = rules.find(visit.table().establishmentId())
                .orElseGet(() -> PresenceRule.empty(visit.table().establishmentId(), now));

        TableSession session = visit.session();
        if (!session.isPresent()) {
            var proved = rule.check(command.clientIp(), command.position());
            if (proved.isPresent()) {
                session = session.confirmPresence(proved.get(), now);
                sessions.save(session);
            }
        }
        return new PresenceStatus(session.isPresent(), session.presenceMethod(), rule.location() != null,
                rule.wifiName());
    }
}
