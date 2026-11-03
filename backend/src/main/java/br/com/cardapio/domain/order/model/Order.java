package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Um envio do carrinho: um celular, um momento, alguns itens. Os itens (OrderItem) ficam à
 * parte, cada um com o seu status, porque a cozinha prepara item a item.
 *
 * @param number     o número do pedido no dia, no fuso do restaurante (nº 1, nº 2…), para a equipe falar dele
 * @param approvedBy quem confirmou; null se foi direto ou ainda não foi confirmado
 */
public record Order(
        OrderId id,
        EstablishmentId establishmentId,
        TabId tabId,
        DiningTableId tableId,
        TableSessionId sessionId,
        int number,
        OrderStatus status,
        Instant createdAt,
        UserId approvedBy,
        Instant approvedAt
) {

    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        if (number < 1) {
            throw new IllegalArgumentException("O número do pedido começa em 1");
        }
    }

    /** O pedido novo: direto para a cozinha, ou parado na confirmação do garçom. */
    public static Order place(EstablishmentId establishmentId, TabId tabId, DiningTableId tableId,
                              TableSessionId sessionId, int number, boolean needsApproval, Instant now) {
        return new Order(OrderId.newId(), establishmentId, tabId, tableId, sessionId, number,
                needsApproval ? OrderStatus.PENDING_APPROVAL : OrderStatus.SENT, now, null, null);
    }
}
