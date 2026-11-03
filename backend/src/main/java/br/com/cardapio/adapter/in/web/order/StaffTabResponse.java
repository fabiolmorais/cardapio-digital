package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.StaffTab;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.order.model.OrderStatus;
import br.com.cardapio.domain.order.model.TabStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "StaffTab")
record StaffTabResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) TabStatus status,
        @Schema(requiredMode = REQUIRED) Instant openedAt,
        Instant closedAt,
        @Schema(requiredMode = REQUIRED, description = "Aberta: a soma de agora. Fechada: o que foi cobrado")
        long totalCents,
        @Schema(description = "Fechada: como a mesa pagou") PaymentMethod paymentMethod,
        @Schema(description = "Aberta, com a conta pedida: a forma que a mesa escolheu")
        PaymentMethod billRequestedWith,
        @Schema(requiredMode = REQUIRED) List<Order> orders
) {

    @Schema(name = "StaffTabOrder")
    record Order(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) int number,
            @Schema(requiredMode = REQUIRED) OrderStatus status,
            @Schema(requiredMode = REQUIRED) Instant createdAt,
            @Schema(requiredMode = REQUIRED) List<TableOrderResponse.Item> items
    ) {
    }

    static StaffTabResponse from(StaffTab t) {
        return new StaffTabResponse(t.id().value(), t.tableLabel(), t.sectorName(), t.status(), t.openedAt(),
                t.closedAt(), t.total().cents(), t.paymentMethod(), t.billRequestedWith(),
                t.orders().stream().map(o -> new Order(o.id().value(), o.number(), o.status(), o.createdAt(),
                        o.items().stream().map(TableOrderResponse.Item::from).toList())).toList());
    }
}
