package br.com.cardapio.support;

import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import br.com.cardapio.domain.tablesession.model.TableSession;

import java.time.Instant;
import java.util.HashMap;
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
    public int unlockTable(EstablishmentId establishmentId, DiningTableId tableId, UserId by, Instant now) {
        int unlocked = 0;
        for (TableSession session : Map.copyOf(saved).values()) {
            if (session.establishmentId().equals(establishmentId) && session.isFor(tableId) && session.isActive(now)
                    && !session.isPresent()) {
                save(session.unlock(by, now));
                unlocked++;
            }
        }
        return unlocked;
    }
}
