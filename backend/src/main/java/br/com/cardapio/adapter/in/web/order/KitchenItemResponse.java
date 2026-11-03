package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "KitchenItem")
record KitchenItemResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) UUID orderId,
        @Schema(requiredMode = REQUIRED, description = "O número do pedido no dia") int orderNumber,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) String productName,
        @Schema(requiredMode = REQUIRED) int quantity,
        String note,
        @Schema(requiredMode = REQUIRED) OrderItemStatus status,
        @Schema(requiredMode = REQUIRED, description = "Quando foi pedido") Instant createdAt,
        @Schema(description = "Quando a cozinha começou") Instant preparingAt,
        @Schema(description = "Quando ficou pronto") Instant readyAt
) {
    static KitchenItemResponse from(KitchenItem i) {
        return new KitchenItemResponse(i.id().value(), i.orderId().value(), i.orderNumber(), i.tableLabel(),
                i.sectorName(), i.productName(), i.quantity(), i.note(), i.status(), i.createdAt(), i.preparingAt(),
                i.readyAt());
    }
}
