package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.menu.model.Product;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.exception.OrderItemStateException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Um item do pedido. O nome e o preço são COPIADOS do cardápio no momento do pedido
 * (especificação): mudar o cardápio depois não muda o que já foi pedido nem a conta.
 *
 * Cada mudança de status guarda a hora; os horários de preparo, entrega e quem entregou entram
 * no Guia 19, com a cozinha e o garçom.
 *
 * @param canceledBy   quem da equipe cancelou; null se foi o próprio cliente (ou se não foi cancelado)
 * @param cancelReason o motivo, que o cliente vê ("acabou o salmão")
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
        String cancelReason
) {

    /** O motivo quando quem cancela é o cliente. */
    public static final String CANCELED_BY_CUSTOMER = "Cancelado pelo cliente";

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
                OrderItemStatus.PENDING, now, null, null, null, null, null, null, null);
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
                CANCELED_BY_CUSTOMER);
    }
}
