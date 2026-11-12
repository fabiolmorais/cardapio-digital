package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.ClosedTab;
import br.com.cardapio.domain.call.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ClosedTab")
record ClosedTabResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) Instant openedAt,
        @Schema(requiredMode = REQUIRED) Instant closedAt,
        @Schema(requiredMode = REQUIRED, description = "O que foi cobrado") long totalCents,
        @Schema(description = "Como a mesa pagou; ausente se fechou sem nada a cobrar") PaymentMethod paymentMethod,
        @Schema(description = "Quem fechou") String closedByName
) {
    static ClosedTabResponse from(ClosedTab t) {
        return new ClosedTabResponse(t.id().value(), t.tableLabel(), t.sectorName(), t.openedAt(), t.closedAt(),
                t.total().cents(), t.paymentMethod(), t.closedByName());
    }
}
