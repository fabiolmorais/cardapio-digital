package br.com.cardapio.domain.order.event;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.event.TableEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Uma comanda do balcão abriu ou mudou de nome (ADR 0015): a equipe atualiza a lista do balcão. Só
 * os ids; a tela busca o resto pela API.
 */
public record CounterTabChanged(
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TabId tabId,
        Instant occurredAt
) implements TableEvent {

    public CounterTabChanged {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(occurredAt);
    }
}
