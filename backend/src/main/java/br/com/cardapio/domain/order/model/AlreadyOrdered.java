package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.menu.model.ItemChoices;
import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Instant;
import java.util.Objects;

/**
 * Um item que a mesa acabou de pedir ou que ainda não chegou: o que o garçom precisa ver para não
 * lançar o mesmo pedido de novo.
 *
 * @param byStaff      foi a equipe que lançou (senão, um celular da mesa)
 * @param placedByName o nome de quem lançou; null se veio de um celular
 */
public record AlreadyOrdered(ProductId productId, String productName, int quantity, ItemChoices choices,
                             OrderItemStatus status, Instant orderedAt, boolean byStaff, String placedByName) {

    public AlreadyOrdered {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(orderedAt);
    }
}
