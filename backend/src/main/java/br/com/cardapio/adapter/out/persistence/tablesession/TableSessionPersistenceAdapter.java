package br.com.cardapio.adapter.out.persistence.tablesession;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class TableSessionPersistenceAdapter implements TableSessionPort {

    private final TableSessionJpaRepository sessions;
    private final EstablishmentFilter establishmentFilter;

    TableSessionPersistenceAdapter(TableSessionJpaRepository sessions, EstablishmentFilter establishmentFilter) {
        this.sessions = sessions;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public void save(TableSession s) {
        establishmentFilter.enable(s.establishmentId());
        sessions.save(new TableSessionJpaEntity(s.id().value(), s.establishmentId().value(), s.tableId().value(),
                s.tokenHash().value(), s.createdAt(), s.lastSeenAt(), s.expiresAt(), s.endedAt()));
    }

    /** Sem o filtro por restaurante: ainda não se sabe de qual é. O token diz (como no findActiveByToken da mesa). */
    @Override
    public Optional<TableSession> findByTokenHash(TokenHash tokenHash) {
        return sessions.findByTokenHash(tokenHash.value()).map(e -> new TableSession(new TableSessionId(e.getId()),
                new EstablishmentId(e.getEstablishmentId()), new DiningTableId(e.getTableId()),
                new TokenHash(e.getTokenHash()), e.getCreatedAt(), e.getLastSeenAt(), e.getExpiresAt(),
                e.getEndedAt()));
    }
}
