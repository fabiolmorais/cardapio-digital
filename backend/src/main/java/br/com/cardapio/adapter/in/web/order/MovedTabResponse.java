package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.in.order.MovedTab;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MovedTab", description = "A comanda que continua, já na mesa de destino")
record MovedTabResponse(
        @Schema(requiredMode = REQUIRED, description = "Na junção, a comanda da mesa de destino") UUID tabId,
        @Schema(requiredMode = REQUIRED) UUID tableId,
        @Schema(requiredMode = REQUIRED, description = "As duas comandas se juntaram") boolean merged
) {

    static MovedTabResponse from(MovedTab moved) {
        return new MovedTabResponse(moved.tabId().value(), moved.tableId().value(), moved.merged());
    }
}
