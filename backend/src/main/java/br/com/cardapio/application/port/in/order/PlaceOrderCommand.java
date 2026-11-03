package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.order.model.OrderLine;

import java.util.List;
import java.util.Objects;

/** "Enviar pedido": o carrinho deste celular. */
public record PlaceOrderCommand(String tableToken, String sessionToken, List<OrderLine> lines) {

    public PlaceOrderCommand {
        lines = List.copyOf(Objects.requireNonNull(lines));
    }
}
