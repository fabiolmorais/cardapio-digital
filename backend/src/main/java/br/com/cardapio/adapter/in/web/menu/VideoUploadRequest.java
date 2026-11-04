package br.com.cardapio.adapter.in.web.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "VideoUploadRequest", description = "O vídeo que o navegador vai enviar")
record VideoUploadRequest(
        @Schema(requiredMode = REQUIRED, example = "video/mp4") @NotBlank String contentType,
        @Schema(requiredMode = REQUIRED, description = "Tamanho em bytes (até 200 MB)", example = "48331112")
        @NotNull @Positive Long sizeBytes
) {
}
