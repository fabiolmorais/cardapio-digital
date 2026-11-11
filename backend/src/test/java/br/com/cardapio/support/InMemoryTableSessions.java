package br.com.cardapio.support;

import br.com.cardapio.application.port.out.tablesession.BlockedPhoneView;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** As sessões da mesa na memória, para os testes dos serviços da mesa (no lugar do banco). */
public final class InMemoryTableSessions implements TableSessionPort {

    /** O que foi gravado, pelo hash do token (como no banco: o token em si nunca é guardado). */
    public final Map<TokenHash, TableSession> saved = new HashMap<>();

    /** Uma sessão aberta agora, ainda sem a presença confirmada. */
    public TableSession given(EstablishmentId establishmentId, DiningTableId tableId, String token, Instant now) {
        TableSession session = TableSession.open(establishmentId, tableId, TokenHash.of(token), now);
        save(session);
        return session;
    }

    /** Uma sessão já no salão (pelo Wi-Fi): para os testes que não são sobre a presença. */
    public TableSession givenPresent(EstablishmentId establishmentId, DiningTableId tableId, String token,
                                     Instant now) {
        TableSession session = given(establishmentId, tableId, token, now).confirmPresence(PresenceMethod.WIFI, now);
        save(session);
        return session;
    }

    public TableSession byToken(String token) {
        return saved.get(TokenHash.of(token));
    }

    @Override
    public void save(TableSession session) {
        saved.put(session.tokenHash(), session);
    }

    @Override
    public Optional<TableSession> findByTokenHash(TokenHash tokenHash) {
        return Optional.ofNullable(saved.get(tokenHash));
    }

    @Override
    public boolean unlockSession(EstablishmentId establishmentId, TableSessionId sessionId, UserId by, Instant now) {
        for (TableSession session : Map.copyOf(saved).values()) {
            if (session.id().equals(sessionId) && session.establishmentId().equals(establishmentId)
                    && session.isActive(now) && !session.isPresent() && !session.isBlocked()) {
                save(session.unlock(by, now));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean block(EstablishmentId establishmentId, TableSessionId sessionId, UserId by, Instant now) {
        for (TableSession session : Map.copyOf(saved).values()) {
            if (session.id().equals(sessionId) && session.establishmentId().equals(establishmentId)
                    && !session.isBlocked()) {
                save(session.block(by, now));
                return true;
            }
        }
        return false;
    }

    private Optional<TableSession> find(EstablishmentId establishmentId, TableSessionId sessionId) {
        return saved.values().stream()
                .filter(s -> s.id().equals(sessionId) && s.establishmentId().equals(establishmentId)).findFirst();
    }

    @Override
    public Optional<DiningTableId> findTable(EstablishmentId establishmentId, TableSessionId sessionId) {
        return find(establishmentId, sessionId).map(TableSession::tableId);
    }

    @Override
    public boolean unblock(EstablishmentId establishmentId, TableSessionId sessionId) {
        Optional<TableSession> blocked = find(establishmentId, sessionId).filter(TableSession::isBlocked);
        blocked.ifPresent(s -> save(s.unblock()));
        return blocked.isPresent();
    }

    /** A mesa e o setor não estão aqui: a lista leva só a sessão, a mesa e quando foi bloqueada. */
    @Override
    public ResultPage<BlockedPhoneView> findBlocked(EstablishmentId establishmentId, Instant now, PageQuery page) {
        List<BlockedPhoneView> blocked = saved.values().stream()
                .filter(s -> s.establishmentId().equals(establishmentId) && s.isBlocked() && s.isActive(now))
                .sorted(Comparator.comparing(TableSession::blockedAt).reversed())
                .map(s -> new BlockedPhoneView(s.id(), s.tableId(), null, null, s.blockedAt(), null))
                .toList();
        return new ResultPage<>(blocked, page.page(), page.size(), blocked.size());
    }

    @Override
    public int endExpired(Instant now) {
        int ended = 0;
        for (TableSession s : Map.copyOf(saved).values()) {
            if (s.endedAt() == null && !s.expiresAt().isAfter(now)) {
                save(new TableSession(s.id(), s.establishmentId(), s.tableId(), s.tokenHash(), s.createdAt(),
                        s.lastSeenAt(), s.expiresAt(), s.expiresAt(), s.presenceMethod(), s.presenceConfirmedAt(),
                        s.unlockedBy(), s.idleLimit(), s.blockedAt(), s.blockedBy()));
                ended++;
            }
        }
        return ended;
    }

    @Override
    public int endTable(EstablishmentId establishmentId, DiningTableId tableId, Instant now) {
        int ended = 0;
        for (TableSession s : Map.copyOf(saved).values()) {
            if (s.establishmentId().equals(establishmentId) && s.isFor(tableId) && s.endedAt() == null) {
                save(new TableSession(s.id(), s.establishmentId(), s.tableId(), s.tokenHash(), s.createdAt(),
                        s.lastSeenAt(), s.expiresAt(), now, s.presenceMethod(), s.presenceConfirmedAt(),
                        s.unlockedBy(), s.idleLimit(), s.blockedAt(), s.blockedBy()));
                ended++;
            }
        }
        return ended;
    }
}
