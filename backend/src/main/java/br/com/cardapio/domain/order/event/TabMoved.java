package br.com.cardapio.domain.order.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.event.TableEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * A comanda mudou de mesa ou se juntou a outra (ADR 0012). Sai um aviso para cada uma das duas
 * mesas ({@code tableId}): os celulares da mesa de origem vão para a nova, e os da de destino
 * buscam de novo os pedidos. Só ids, como os outros eventos.
 *
 * @param tabId a comanda que continua
 */
public record TabMoved(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        DiningTableId fromTableId,
        DiningTableId toTableId,
        TabId tabId,
        Instant occurredAt
) implements TableEvent {

    public TabMoved {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(fromTableId);
        Objects.requireNonNull(toTableId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(occurredAt);
    }

    /** Para os celulares desta mesa, a comanda foi embora (eles seguem para a mesa nova)? */
    public boolean leftThisTable() {
        return tableId.equals(fromTableId);
    }
}
