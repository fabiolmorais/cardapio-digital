package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Instant;
import java.util.Objects;

/**
 * Um item que passou pela cozinha: quando chegou (a confirmação do garçom, quando o pedido precisou
 * dela; senão, o próprio pedido), quando ficou pronto e quando chegou à mesa.
 *
 * @param ready     null enquanto não ficou pronto
 * @param delivered null enquanto não foi entregue
 */
public record KitchenItem(ProductId productId, String name, String category, Instant received, Instant ready,
                          Instant delivered) {

    public KitchenItem {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(received);
    }
}
