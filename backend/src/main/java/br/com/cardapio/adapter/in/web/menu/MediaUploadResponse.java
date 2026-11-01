package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.MediaUpload;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MediaUpload",
        description = "Envie o arquivo com um PUT na url, com estes cabeçalhos; depois avise com o uploadId")
record MediaUploadResponse(
        @Schema(requiredMode = REQUIRED) UUID uploadId,
        @Schema(requiredMode = REQUIRED, description = "URL assinada, de uso único e curta validade") String url,
        @Schema(requiredMode = REQUIRED, example = "PUT") String method,
        @Schema(requiredMode = REQUIRED) Map<String, String> headers,
        @Schema(requiredMode = REQUIRED) Instant expiresAt
) {
    static MediaUploadResponse from(MediaUpload upload) {
        return new MediaUploadResponse(upload.uploadId(), upload.url(), "PUT", upload.headers(), upload.expiresAt());
    }
}
