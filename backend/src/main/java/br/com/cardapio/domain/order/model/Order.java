package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.OrderStateException;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Um envio do carrinho: um celular, um momento, alguns itens. Os itens (OrderItem) ficam à
 * parte, cada um com o seu status, porque a cozinha prepara item a item.
 *
 * @param number     o número do pedido no dia, no fuso do restaurante (nº 1, nº 2…), para a equipe falar dele
 * @param approvedBy quem confirmou ou recusou; null se foi direto ou ainda não foi respondido
 * @param approvedAt quando confirmou ou recusou
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

    /** O garçom confirma: o pedido vai para a cozinha. Só o que está parado na confirmação. */
    public Order approve(UserId by, Instant now) {
        Objects.requireNonNull(by);
        requireWaiting();
        return new Order(id, establishmentId, tabId, tableId, sessionId, number, OrderStatus.SENT, createdAt, by, now);
    }

    /**
     * O garçom recusa (um trote, um engano): o pedido não vai para a cozinha nem para a conta. Os
     * itens são cancelados à parte, pelo serviço, com o motivo {@link OrderItem#REJECTED_BY_STAFF}.
     */
    public Order reject(UserId by, Instant now) {
        Objects.requireNonNull(by);
        requireWaiting();
        return new Order(id, establishmentId, tabId, tableId, sessionId, number, OrderStatus.REJECTED, createdAt, by,
                now);
    }

    private void requireWaiting() {
        if (status != OrderStatus.PENDING_APPROVAL) {
            throw new OrderStateException(status == OrderStatus.SENT
                    ? "Este pedido já foi para a cozinha"
                    : "Este pedido já foi recusado");
        }
    }
}
