package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "OrderSettingsRequest")
record OrderSettingsRequest(
        @Schema(requiredMode = REQUIRED, description = "Desligado, a mesa só chama o garçom e vê o cardápio")
        boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED, description = "Todo pedido espera o garçom confirmar")
        boolean approvalRequired,
        @Schema(description = "Acima deste valor (em centavos), o pedido espera o garçom; vazio = sem limite",
                example = "30000")
        @Min(100) Long maxAmountCents,
        @Schema(description = "Acima desta quantidade de itens, também; vazio = sem limite", example = "10")
        @Min(1) @Max(100) Integer maxItems
) {
}
