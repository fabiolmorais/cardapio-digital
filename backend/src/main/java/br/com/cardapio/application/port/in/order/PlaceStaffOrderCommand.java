package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.OrderLine;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * "Enviar pedido" pela equipe.
 *
 * @param clientRequestId     o código deste envio: repetir o mesmo código não cria outro pedido
 * @param round               true: uma rodada do rodízio da comanda
 * @param confirmedDuplicates os produtos que quem lançou viu no aviso e confirmou que são outro pedido
 */
public record PlaceStaffOrderCommand(EstablishmentId establishmentId, UserId placedBy, DiningTableId tableId,
                                     TabId tabId, UUID clientRequestId, List<OrderLine> lines, boolean round,
                                     Set<ProductId> confirmedDuplicates) {

    /** Na mesa: a comanda dela (abre no primeiro pedido). */
    public PlaceStaffOrderCommand(EstablishmentId establishmentId, UserId placedBy, DiningTableId tableId,
                                  UUID clientRequestId, List<OrderLine> lines, boolean round,
                                  Set<ProductId> confirmedDuplicates) {
        this(establishmentId, placedBy, tableId, null, clientRequestId, lines, round, confirmedDuplicates);
    }

    /** No balcão (ADR 0015): a comanda do cliente, que já existe. */
    public static PlaceStaffOrderCommand atCounter(EstablishmentId establishmentId, UserId placedBy, TabId tabId,
                                                   UUID clientRequestId, List<OrderLine> lines,
                                                   Set<ProductId> confirmedDuplicates) {
        return new PlaceStaffOrderCommand(establishmentId, placedBy, null, Objects.requireNonNull(tabId),
                clientRequestId, lines, false, confirmedDuplicates);
    }

    public PlaceStaffOrderCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(placedBy);
        if ((tableId == null) == (tabId == null)) {
            throw new IllegalArgumentException("o pedido vai para uma mesa ou para uma comanda do balcão");
        }
        Objects.requireNonNull(clientRequestId);
        lines = List.copyOf(Objects.requireNonNull(lines));
        confirmedDuplicates = Set.copyOf(Objects.requireNonNull(confirmedDuplicates));
    }
}
