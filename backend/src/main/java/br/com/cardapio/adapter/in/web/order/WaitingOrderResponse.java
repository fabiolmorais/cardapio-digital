package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.WaitingOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "WaitingOrder")
record WaitingOrderResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, description = "O número do pedido no dia") int number,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) Instant createdAt,
        @Schema(requiredMode = REQUIRED, description = "A soma dos itens não cancelados") long totalCents,
        @Schema(requiredMode = REQUIRED) List<TableOrderResponse.Item> items
) {
    static WaitingOrderResponse from(WaitingOrder o) {
        return new WaitingOrderResponse(o.id().value(), o.number(), o.tableLabel(), o.sectorName(), o.createdAt(),
                o.total().cents(), o.items().stream().map(TableOrderResponse.Item::from).toList());
    }
}
