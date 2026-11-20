package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MoveTabRequest")
record MoveTabRequest(
        @Schema(requiredMode = REQUIRED, description = "A mesa de destino; se ela tem comanda aberta, as duas se juntam")
        @NotNull UUID tableId
) {
}
