package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;

import java.time.Instant;
import java.util.Objects;

/**
 * A comanda: junta os pedidos de todos os celulares da mesa, do primeiro pedido até ser fechada.
 * Ela abre sozinha no primeiro pedido (especificação); quem fecha é o garçom (Etapa 9).
 *
 * @param closedAt null enquanto não foi fechada
 * @param closedBy quem fechou; null enquanto não foi fechada
 */
public record Tab(
        TabId id,
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TabStatus status,
        Instant openedAt,
        Instant closedAt,
        UserId closedBy
) {

    public Tab {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(openedAt);
    }

    public static Tab open(EstablishmentId establishmentId, DiningTableId tableId, Instant now) {
        return new Tab(TabId.newId(), establishmentId, tableId, TabStatus.OPEN, now, null, null);
    }
}
