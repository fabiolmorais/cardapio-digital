package br.com.cardapio.domain.order.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.event.TableEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * A comanda da mesa foi fechada: a equipe tira a mesa das abertas, e os celulares da mesa
 * mostram "Conta fechada". Leva só os ids, como os outros eventos.
 */
public record TabClosed(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TabId tabId,
        Instant occurredAt
) implements TableEvent {

    public TabClosed {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(occurredAt);
    }
}
