package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.TableOrders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TableOrders")
record TableOrdersResponse(
        @Schema(requiredMode = REQUIRED, description = "O restaurante aceita pedido pelo celular")
        boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED, description = "A soma dos itens não cancelados da comanda") long totalCents,
        @Schema(requiredMode = REQUIRED, description = "Os pedidos da comanda aberta, do mais antigo ao mais novo")
        List<TableOrderResponse> orders
) {
    static TableOrdersResponse from(TableOrders t) {
        return new TableOrdersResponse(t.orderingEnabled(), t.total().cents(),
                t.orders().stream().map(TableOrderResponse::from).toList());
    }
}
