package br.com.cardapio.adapter.in.web.call;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ReleaseCallRequest", description = "O código que aparece no celular de quem pediu a liberação")
record ReleaseCallRequest(
        @Schema(requiredMode = REQUIRED, example = "4719") @NotBlank @Pattern(regexp = "\\d{4}") String code
) {
}
