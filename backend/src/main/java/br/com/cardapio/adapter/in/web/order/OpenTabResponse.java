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
        int itemsOnTheWay
) {
    static OpenTabResponse from(OpenTab t) {
        return new OpenTabResponse(t.id().value(), t.tableId().value(), t.tableLabel(), t.sectorName(),
                t.openedAt(), t.total().cents(), t.itemsOnTheWay());
    }
}
