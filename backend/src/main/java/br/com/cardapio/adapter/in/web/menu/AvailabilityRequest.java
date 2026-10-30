package br.com.cardapio.adapter.in.web.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "AvailabilityRequest")
record AvailabilityRequest(
        @Schema(requiredMode = REQUIRED, description = "false: \"Esgotado hoje\"") @NotNull Boolean available
) {
}
