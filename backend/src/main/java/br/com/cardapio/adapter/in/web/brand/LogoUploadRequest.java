package br.com.cardapio.adapter.in.web.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "LogoUploadRequest")
record LogoUploadRequest(
        @Schema(requiredMode = REQUIRED, example = "image/png") @NotBlank String contentType,
        @Schema(requiredMode = REQUIRED, description = "Tamanho em bytes (até 2 MB)") @NotNull @Positive Long sizeBytes
) {
}
