package br.com.cardapio.adapter.in.web.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "CancelItemRequest")
record CancelItemRequest(
        @Schema(requiredMode = REQUIRED, description = "O motivo, que o cliente vê", example = "Acabou o salmão")
        @NotBlank @Size(max = 140) String reason,
        @Schema(description = "Quantos cancelar (a cozinha só tem para parte); ausente = o item todo", example = "5")
        @Min(1) @Max(20) Integer quantity
) {
}
