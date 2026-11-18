package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PlaceStaffOrderRequest")
record PlaceStaffOrderRequest(
        @Schema(requiredMode = REQUIRED, description = "Gerado pela tela a cada envio: repetir não cria outro pedido")
        @NotNull UUID clientRequestId,
        @Schema(requiredMode = REQUIRED) @NotNull @Size(min = 1, max = 30)
        List<PlaceOrderRequest.@NotNull @Valid Line> items,
        @Schema(requiredMode = REQUIRED, description = "Uma rodada do rodízio da comanda") boolean round,
        @Schema(description = "Os produtos do aviso que quem lança confirmou que são outro pedido")
        @Size(max = 30) List<@NotNull UUID> confirmedDuplicates
) {
}
