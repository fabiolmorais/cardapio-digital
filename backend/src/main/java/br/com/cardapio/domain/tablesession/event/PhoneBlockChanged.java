package br.com.cardapio.domain.tablesession.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.event.TableEvent;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.Objects;

/**
 * A equipe bloqueou ou desbloqueou um celular da mesa. A mesa confere de novo a presença (o
 * celular desbloqueado volta a ter os botões na hora) e o painel atualiza a lista de bloqueados.
 * Só ids, como os outros eventos.
 */
public record PhoneBlockChanged(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TableSessionId sessionId,
        boolean blocked,
        Instant occurredAt
) implements TableEvent {

    public PhoneBlockChanged {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(occurredAt);
    }
}
