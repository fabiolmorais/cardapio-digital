package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.application.port.in.tablesession.StartTableSessionUseCase;
import br.com.cardapio.application.port.in.tablesession.TableSessionStarted;
import br.com.cardapio.application.port.out.floor.DiningTablePort;
import br.com.cardapio.application.port.out.order.TabMovePort;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.application.port.out.security.RandomTokenGenerator;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.service.floor.PublicTableLookup;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.presence.model.PresenceRule;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.TableSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class StartTableSessionService implements StartTableSessionUseCase {

    private final DiningTablePort tables;
    private final TableSessionPort sessions;
    private final PresenceRulePort rules;
    private final RandomTokenGenerator tokens;
    private final TabMovePort moves;
    private final Clock clock;

    public StartTableSessionService(DiningTablePort tables, TableSessionPort sessions, PresenceRulePort rules,
                                    RandomTokenGenerator tokens, TabMovePort moves, Clock clock) {
        this.moves = moves;
        this.tables = tables;
        this.sessions = sessions;
        this.rules = rules;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TableSessionStarted start(String tableToken, String currentToken) {
        DiningTable table = PublicTableLookup.resolve(tables, tableToken).table();
        Instant now = clock.instant();

        Optional<TableSession> active = currentToken == null || currentToken.isBlank()
                ? Optional.empty()
                : sessions.findByTokenHash(TokenHash.of(currentToken)).filter(s -> s.isActive(now));

        // A comanda deste celular mudou de mesa depois que ele chegou (ADR 0012): em vez de começar
        // uma visita nova na mesa antiga, a página vai para a mesa nova, onde a sessão já está.
        Optional<String> movedTo = active
                .filter(s -> !s.isFor(table.id()) && s.establishmentId().equals(table.establishmentId()))
                .filter(s -> moves.movedBetween(table.establishmentId(), table.id(), s.tableId(), s.createdAt()))
                .flatMap(s -> tables.find(s.establishmentId(), s.tableId()))
                .filter(DiningTable::active)
                .map(t -> t.token().value());
        if (movedTo.isPresent()) {
            return new TableSessionStarted(currentToken, active.get().expiresAt(), true, movedTo.get());
        }

        // O celular já tem uma sessão válida NESTA mesa? Continua nela (e o prazo recomeça).
        Optional<TableSession> current = active.filter(s -> s.isFor(table.id()));
        if (current.isPresent()) {
            TableSession touched = current.get().touch(now);
            sessions.save(touched);
            return new TableSessionStarted(currentToken, touched.expiresAt(), true);
        }

        // Primeira visita, sessão vencida ou cookie de outra mesa: uma sessão nova, com token novo,
        // e com o tempo sem uso que o restaurante escolheu.
        Duration idleLimit = rules.find(table.establishmentId()).map(PresenceRule::sessionIdleLimit)
                .orElse(PresenceRule.DEFAULT_SESSION_IDLE_LIMIT);
        String token = tokens.generate();
        TableSession session = TableSession.open(table.establishmentId(), table.id(), TokenHash.of(token),
                idleLimit, now);
        sessions.save(session);
        return new TableSessionStarted(token, session.expiresAt(), false);
    }
}
