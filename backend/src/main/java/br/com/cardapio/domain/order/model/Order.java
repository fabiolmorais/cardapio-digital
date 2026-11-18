package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.exception.OrderStateException;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Um envio: de um celular da mesa ou de uma pessoa da equipe, um momento, alguns itens. Os itens
 * (OrderItem) ficam à parte, cada um com o seu status, porque a cozinha prepara item a item.
 *
 * @param origin             quem mandou: um celular da mesa ou alguém da equipe (ADR 0011)
 * @param number             o número do pedido no dia, no fuso do restaurante (nº 1, nº 2…), para a equipe falar dele
 * @param approvedBy         quem confirmou ou recusou; null se foi direto ou ainda não foi respondido
 * @param approvedAt         quando confirmou ou recusou
 * @param clientRequestId    o código do envio, gerado pela tela da equipe; null no pedido do celular
 * @param duplicateConfirmed o garçom viu o aviso de item repetido e confirmou que é outro pedido
 */
public record Order(
        OrderId id,
        EstablishmentId establishmentId,
        TabId tabId,
        DiningTableId tableId,
        OrderOrigin origin,
        int number,
        OrderStatus status,
        Instant createdAt,
        UserId approvedBy,
        Instant approvedAt,
        UUID clientRequestId,
        boolean duplicateConfirmed
) {

    /**
     * O pedido do garçom feito até este tempo antes da primeira sessão ativa ainda é da visita: quem
     * escaneia depois vê o que já pediu, e a comanda esquecida de outro dia continua escondida.
     */
    public static final Duration STAFF_ORDER_VISIT_WINDOW = TableSession.IDLE_LIMIT;

    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        if (number < 1) {
            throw new IllegalArgumentException("O número do pedido começa em 1");
        }
    }

    /** O pedido novo do celular: direto para a cozinha, ou parado na confirmação do garçom. */
    public static Order place(EstablishmentId establishmentId, TabId tabId, DiningTableId tableId,
                              TableSessionId sessionId, int number, boolean needsApproval, Instant now) {
        return new Order(OrderId.newId(), establishmentId, tabId, tableId, new OrderOrigin.Phone(sessionId), number,
                needsApproval ? OrderStatus.PENDING_APPROVAL : OrderStatus.SENT, now, null, null, null, false);
    }

    /** O pedido lançado pela equipe: vai direto para a cozinha, sem a confirmação feita para o cliente anônimo. */
    public static Order placeByStaff(EstablishmentId establishmentId, TabId tabId, DiningTableId tableId, UserId by,
                                     int number, UUID clientRequestId, boolean duplicateConfirmed, Instant now) {
        return new Order(OrderId.newId(), establishmentId, tabId, tableId, new OrderOrigin.Staff(by), number,
                OrderStatus.SENT, now, null, null, Objects.requireNonNull(clientRequestId), duplicateConfirmed);
    }

    public boolean byStaff() {
        return origin instanceof OrderOrigin.Staff;
    }

    /** Foi este celular que mandou? */
    public boolean placedBy(TableSessionId sessionId) {
        return origin instanceof OrderOrigin.Phone phone && phone.sessionId().equals(sessionId);
    }

    /** O pedido entra na visita que começou neste instante? Ver {@link #STAFF_ORDER_VISIT_WINDOW}. */
    public boolean belongsToVisit(Instant visitStart) {
        Instant from = byStaff() ? visitStart.minus(STAFF_ORDER_VISIT_WINDOW) : visitStart;
        return !createdAt.isBefore(from);
    }

    /** O garçom confirma: o pedido vai para a cozinha. Só o que está parado na confirmação. */
    public Order approve(UserId by, Instant now) {
        Objects.requireNonNull(by);
        requireWaiting();
        return answered(OrderStatus.SENT, by, now);
    }

    /**
     * O garçom recusa (um trote, um engano): o pedido não vai para a cozinha nem para a conta. Os
     * itens são cancelados à parte, pelo serviço, com o motivo {@link OrderItem#REJECTED_BY_STAFF}.
     */
    public Order reject(UserId by, Instant now) {
        Objects.requireNonNull(by);
        requireWaiting();
        return answered(OrderStatus.REJECTED, by, now);
    }

    private Order answered(OrderStatus newStatus, UserId by, Instant now) {
        return new Order(id, establishmentId, tabId, tableId, origin, number, newStatus, createdAt, by, now,
                clientRequestId, duplicateConfirmed);
    }

    private void requireWaiting() {
        if (status != OrderStatus.PENDING_APPROVAL) {
            throw new OrderStateException(status == OrderStatus.SENT
                    ? "Este pedido já foi para a cozinha"
                    : "Este pedido já foi recusado");
        }
    }
}
