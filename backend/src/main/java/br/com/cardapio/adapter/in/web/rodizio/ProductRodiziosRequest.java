package br.com.cardapio.adapter.in.web.rodizio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ProductRodiziosRequest", description = "Os rodízios em que o produto fica (vazio: em nenhum)")
record ProductRodiziosRequest(
        @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 100) List<@NotNull UUID> rodizioIds
) {
}
