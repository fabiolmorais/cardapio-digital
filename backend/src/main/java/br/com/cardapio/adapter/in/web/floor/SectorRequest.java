package br.com.cardapio.adapter.in.web.floor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Criar e renomear setor pedem a mesma coisa: o nome. */
@Schema(name = "SectorRequest")
record SectorRequest(
        @Schema(requiredMode = REQUIRED, example = "Deck") @NotBlank @Size(max = 40) String name
) {
}
