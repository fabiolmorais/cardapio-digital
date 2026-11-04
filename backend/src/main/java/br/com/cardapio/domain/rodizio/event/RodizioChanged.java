package br.com.cardapio.domain.rodizio.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.event.TableEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * O rodízio da mesa foi ativado ou mudou de pessoas: o celular mostra a aba Rodízio e a conta
 * nova, e a equipe atualiza a comanda. Só ids, como os outros eventos.
 */
public record RodizioChanged(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TabId tabId,
        Instant occurredAt
) implements TableEvent {

    public RodizioChanged {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(occurredAt);
    }
}
