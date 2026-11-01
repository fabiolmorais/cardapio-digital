package br.com.cardapio.adapter.in.web.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "AddMediaRequest")
record AddMediaRequest(
        @Schema(requiredMode = REQUIRED, description = "O uploadId recebido no primeiro passo") @NotNull UUID uploadId
) {
}
