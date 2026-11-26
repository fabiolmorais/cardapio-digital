package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.OpenTab;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "OpenTab")
record OpenTabResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) UUID tableId,
        @Schema(requiredMode = REQUIRED) String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED) Instant openedAt,
        @Schema(requiredMode = REQUIRED, description = "A soma dos itens não cancelados até agora") long totalCents,
        @Schema(requiredMode = REQUIRED, description = "Itens ainda a caminho (na fila, em preparo, prontos)")
        int itemsOnTheWay,
        @Schema(requiredMode = REQUIRED, description = "A comanda está no balcão (ADR 0015)") boolean atCounter,
        @Schema(description = "No balcão: como a equipe chama o cliente (continua se a comanda for para uma mesa)")
        String customerName,
        @Schema(description = "O número do cartão, se a casa usa") String cardNumber
) {
    static OpenTabResponse from(OpenTab t) {
        return new OpenTabResponse(t.id().value(), t.tableId().value(), t.tableLabel(), t.sectorName(),
                t.openedAt(), t.total().cents(), t.itemsOnTheWay(), t.atCounter(),
                t.guest() == null ? null : t.guest().name(), t.guest() == null ? null : t.guest().card());
    }
}
