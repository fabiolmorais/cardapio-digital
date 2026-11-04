package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.OrderLine;

import java.util.List;
import java.util.Objects;

/** "Enviar pedido": o carrinho deste celular. */
/**
 * @param round true: uma rodada do rodízio (os itens incluídos, a preço zero); false: o carrinho
 */
public record PlaceOrderCommand(String tableToken, String sessionToken, List<OrderLine> lines, boolean round) {

    public PlaceOrderCommand {
        lines = List.copyOf(Objects.requireNonNull(lines));
    }

    /** O carrinho, com preço. */
    public PlaceOrderCommand(String tableToken, String sessionToken, List<OrderLine> lines) {
        this(tableToken, sessionToken, lines, false);
    }
}
