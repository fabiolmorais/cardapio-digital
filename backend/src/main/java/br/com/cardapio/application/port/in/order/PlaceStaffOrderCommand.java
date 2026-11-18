package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.menu.model.ProductId;
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
                                     UUID clientRequestId, List<OrderLine> lines, boolean round,
                                     Set<ProductId> confirmedDuplicates) {

    public PlaceStaffOrderCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(placedBy);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(clientRequestId);
        lines = List.copyOf(Objects.requireNonNull(lines));
        confirmedDuplicates = Set.copyOf(Objects.requireNonNull(confirmedDuplicates));
    }
}
