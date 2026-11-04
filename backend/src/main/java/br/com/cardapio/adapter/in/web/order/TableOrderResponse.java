package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.TableOrder;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TableOrder")
record TableOrderResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, description = "O número do pedido no dia") int number,
        @Schema(requiredMode = REQUIRED) OrderStatus status,
        @Schema(requiredMode = REQUIRED) Instant createdAt,
        @Schema(requiredMode = REQUIRED, description = "Foi este celular que mandou (só ele cancela os itens)")
        boolean mine,
        @Schema(requiredMode = REQUIRED) List<Item> items
) {

    @Schema(name = "TableOrderItem")
    record Item(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) String productName,
            @Schema(requiredMode = REQUIRED, description = "O preço copiado na hora do pedido") long unitPriceCents,
            @Schema(requiredMode = REQUIRED) int quantity,
            String note,
            @Schema(requiredMode = REQUIRED) OrderItemStatus status,
            @Schema(description = "Por que foi cancelado") String cancelReason,
            @Schema(requiredMode = REQUIRED, description = "Veio numa rodada do rodízio (preço zero)") boolean rodizio
    ) {
        static Item from(TableOrder.Item i) {
            return new Item(i.id().value(), i.productName(), i.unitPrice().cents(), i.quantity(), i.note(),
                    i.status(), i.cancelReason(), i.rodizio());
        }
    }

    static TableOrderResponse from(TableOrder o) {
        return new TableOrderResponse(o.id().value(), o.number(), o.status(), o.createdAt(), o.mine(),
                o.items().stream().map(Item::from).toList());
    }
}
