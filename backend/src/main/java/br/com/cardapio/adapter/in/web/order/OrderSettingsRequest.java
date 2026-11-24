package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import br.com.cardapio.domain.order.model.ServiceDestination;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "OrderSettingsRequest")
record OrderSettingsRequest(
        @Schema(requiredMode = REQUIRED, description = "Desligado, a mesa só chama o garçom e vê o cardápio")
        boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED, description = "Todo pedido espera o garçom confirmar")
        boolean approvalRequired,
        @Schema(description = "A equipe do salão pode lançar pedidos nas mesas; vazio mantém como está")
        Boolean staffOrderingEnabled,
        @Schema(description = "Acima deste valor (em centavos), o pedido espera o garçom; vazio = sem limite",
                example = "30000")
        @Min(100) Long maxAmountCents,
        @Schema(description = "Acima desta quantidade de itens, também; vazio = sem limite", example = "10")
        @Min(1) @Max(100) Integer maxItems,
        @Schema(description = "A taxa de serviço opcional (os 10% do garçom); vazio mantém como está")
        @Valid ServiceChargeRequest serviceCharge
) {

    @Schema(name = "ServiceChargeSettings")
    record ServiceChargeRequest(
            @Schema(requiredMode = REQUIRED, description = "Cobra a taxa de serviço") boolean enabled,
            @Schema(requiredMode = REQUIRED, description = "O percentual, de 1 a 20", example = "10")
            @Min(1) @Max(20) int percent,
            @Schema(requiredMode = REQUIRED, description = "Para quem fica registrada")
            @NotNull ServiceDestination destination,
            @Schema(description = "A mesa pode escolher outro valor no lugar do percentual (vazio: não pode)")
            Boolean customAllowed
    ) {
    }
}
