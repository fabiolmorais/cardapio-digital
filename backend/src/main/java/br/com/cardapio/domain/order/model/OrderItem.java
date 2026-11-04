package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Um item do pedido. O nome e o preço são COPIADOS do cardápio no momento do pedido
 * (especificação): mudar o cardápio depois não muda o que já foi pedido nem a conta.
 *
 * Cada mudança de status guarda a hora (o "há quanto tempo" da cozinha e, depois, os relatórios).
 * A cozinha marca em preparo e pronto; o garçom marca entregue; a equipe cancela com motivo.
 *
 * @param canceledBy   quem da equipe cancelou; null se foi o próprio cliente (ou se não foi cancelado)
 * @param cancelReason o motivo, que o cliente vê ("acabou o salmão")
 * @param tabRodizioId o rodízio da comanda, se o item veio numa rodada (preço zero); senão null
 */
public record OrderItem(
        OrderItemId id,
        EstablishmentId establishmentId,
        OrderId orderId,
        ProductId productId,
        String productName,
        Money unitPrice,
        int quantity,
        String note,
        OrderItemStatus status,
        Instant createdAt,
        Instant preparingAt,
        Instant readyAt,
        Instant deliveredAt,
        UserId deliveredBy,
        Instant canceledAt,
        UserId canceledBy,
        String cancelReason,
        TabRodizioId tabRodizioId
) {

    /** O motivo quando quem cancela é o cliente. */
    public static final String CANCELED_BY_CUSTOMER = "Cancelado pelo cliente";
    /** O motivo dos itens de um pedido que o garçom recusou. */
    public static final String REJECTED_BY_STAFF = "Pedido recusado pelo garçom";
    /** O tamanho máximo do motivo do cancelamento (a coluna cancel_reason). */
    public static final int MAX_REASON = 140;

    public OrderItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(productId);
        Objects.requireNonNull(productName);
        Objects.requireNonNull(unitPrice);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
    }

    /** O item novo, com o nome e o preço de agora (o promocional, se estiver em promoção). */
    public static OrderItem from(OrderId orderId, Product product, OrderLine line, Instant now) {
        return new OrderItem(OrderItemId.newId(), product.establishmentId(), orderId, product.id(),
                product.details().name(), product.details().currentPrice(), line.quantity(), line.note(),
                OrderItemStatus.PENDING, now, null, null, null, null, null, null, null, null);
    }

    /**
     * O item de uma rodada do rodízio: o nome vem do cardápio, mas o preço é zero, porque o
     * rodízio é cobrado por pessoa (especificação).
     */
    public static OrderItem inRound(OrderId orderId, Product product, OrderLine line, TabRodizioId tabRodizioId,
                                    Instant now) {
        return new OrderItem(OrderItemId.newId(), product.establishmentId(), orderId, product.id(),
                product.details().name(), Money.ZERO, line.quantity(), line.note(), OrderItemStatus.PENDING, now,
                null, null, null, null, null, null, null, Objects.requireNonNull(tabRodizioId));
    }

    /** Veio numa rodada do rodízio? */
    public boolean inRodizio() {
        return tabRodizioId != null;
    }

    /** O preço do item vezes a quantidade. */
    public Money subtotal() {
        return unitPrice.times(quantity);
    }

    /**
     * O cliente desiste do item. Só enquanto ele está na fila (especificação): depois que a
     * cozinha começa, só a equipe cancela.
     */
    public OrderItem cancelByCustomer(Instant now) {
        if (status != OrderItemStatus.PENDING) {
            throw new OrderItemStateException(status == OrderItemStatus.CANCELED
                    ? "Este item já foi cancelado"
                    : "A cozinha já começou este item. Para cancelar, fale com o garçom.");
        }
        return new OrderItem(id, establishmentId, orderId, productId, productName, unitPrice, quantity, note,
                OrderItemStatus.CANCELED, createdAt, preparingAt, readyAt, deliveredAt, deliveredBy, now, null,
                CANCELED_BY_CUSTOMER, tabRodizioId);
    }

    /** "Em preparo": a cozinha começou. Só a partir da fila. */
    public OrderItem start(Instant now) {
        if (status != OrderItemStatus.PENDING) {
            throw new OrderItemStateException(stateMessage());
        }
        return with(OrderItemStatus.PREPARING, now, readyAt, deliveredAt, deliveredBy, canceledAt, canceledBy,
                cancelReason);
    }

    /**
     * "Pronto": saindo da cozinha. Vale da fila também: a bebida que ninguém "prepara" pula
     * direto para pronto, sem passar por em preparo.
     */
    public OrderItem markReady(Instant now) {
        if (status != OrderItemStatus.PENDING && status != OrderItemStatus.PREPARING) {
            throw new OrderItemStateException(stateMessage());
        }
        return with(OrderItemStatus.READY, preparingAt, now, deliveredAt, deliveredBy, canceledAt, canceledBy,
                cancelReason);
    }

    /**
     * "Entregue": chegou à mesa. O garçom pode entregar o que ainda não passou por "pronto" (num
     * bar sem cozinha, ninguém marca os passos de antes); só o que já saiu de cena não volta.
     */
    public OrderItem deliver(UserId by, Instant now) {
        Objects.requireNonNull(by);
        if (!status.isOnTheWay()) {
            throw new OrderItemStateException(stateMessage());
        }
        return with(OrderItemStatus.DELIVERED, preparingAt, readyAt, now, by, canceledAt, canceledBy, cancelReason);
    }

    /**
     * A equipe cancela, em qualquer passo antes da entrega, com o motivo que o cliente vê
     * ("acabou o salmão"). O motivo é obrigatório: é o que explica ao cliente por que não veio.
     */
    public OrderItem cancelByStaff(UserId by, String reason, Instant now) {
        Objects.requireNonNull(by);
        String text = reason == null ? "" : reason.strip();
        if (text.isEmpty() || text.length() > MAX_REASON) {
            throw new InvalidValueException("Diga o motivo do cancelamento, em até 140 letras.");
        }
        if (!status.isOnTheWay()) {
            throw new OrderItemStateException(stateMessage());
        }
        return with(OrderItemStatus.CANCELED, preparingAt, readyAt, deliveredAt, deliveredBy, now, by, text);
    }

    private String stateMessage() {
        return switch (status) {
            case DELIVERED -> "Este item já foi entregue";
            case CANCELED -> "Este item foi cancelado";
            case READY -> "Este item já está pronto";
            case PREPARING -> "Este item já está em preparo";
            case PENDING -> "Este item ainda está na fila";
        };
    }

    private OrderItem with(OrderItemStatus newStatus, Instant preparing, Instant ready, Instant delivered,
                           UserId deliveredByUser, Instant canceled, UserId canceledByUser, String reason) {
        return new OrderItem(id, establishmentId, orderId, productId, productName, unitPrice, quantity, note,
                newStatus, createdAt, preparing, ready, delivered, deliveredByUser, canceled, canceledByUser, reason,
                tabRodizioId);
    }
}
