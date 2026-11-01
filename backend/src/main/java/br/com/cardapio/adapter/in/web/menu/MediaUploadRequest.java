package br.com.cardapio.adapter.in.web.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MediaUploadRequest", description = "O arquivo que o navegador vai enviar")
record MediaUploadRequest(
        @Schema(requiredMode = REQUIRED, example = "image/jpeg") @NotBlank String contentType,
        @Schema(requiredMode = REQUIRED, description = "Tamanho em bytes (até 8 MB)", example = "2483112")
        @NotNull @Positive Long sizeBytes
) {
}
