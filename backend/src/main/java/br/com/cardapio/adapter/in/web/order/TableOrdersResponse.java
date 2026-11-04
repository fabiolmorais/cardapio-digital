package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.rodizio.TableRodizioResponse;
import br.com.cardapio.application.port.in.order.TableOrders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TableOrders")
record TableOrdersResponse(
        @Schema(requiredMode = REQUIRED, description = "O restaurante aceita pedido pelo celular")
        boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED, description = "Os itens não cancelados mais o rodízio (pessoas × preço)")
        long totalCents,
        @Schema(description = "O rodízio da mesa; ausente se ela não está no rodízio") TableRodizioResponse rodizio,
        @Schema(requiredMode = REQUIRED, description = "Os produtos incluídos no rodízio (a aba Rodízio)")
        List<UUID> rodizioProductIds,
        @Schema(requiredMode = REQUIRED, description = "Os pedidos da comanda aberta, do mais antigo ao mais novo")
        List<TableOrderResponse> orders
) {
    static TableOrdersResponse from(TableOrders t) {
        return new TableOrdersResponse(t.orderingEnabled(), t.total().cents(),
                t.rodizio() == null ? null : TableRodizioResponse.from(t.rodizio()),
                t.rodizioProducts().stream().map(p -> p.value()).sorted().toList(),
                t.orders().stream().map(TableOrderResponse::from).toList());
    }
}
