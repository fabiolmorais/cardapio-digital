package br.com.cardapio.adapter.in.web.brand;

import br.com.cardapio.application.port.in.brand.LogoUpload;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "LogoUpload", description = "Envie a logo com um PUT na url, com estes cabeçalhos; depois avise com o uploadId")
record LogoUploadResponse(
        @Schema(requiredMode = REQUIRED) UUID uploadId,
        @Schema(requiredMode = REQUIRED) String url,
        @Schema(requiredMode = REQUIRED, example = "PUT") String method,
        @Schema(requiredMode = REQUIRED) Map<String, String> headers,
        @Schema(requiredMode = REQUIRED) Instant expiresAt
) {
    static LogoUploadResponse from(LogoUpload upload) {
        return new LogoUploadResponse(upload.uploadId(), upload.url(), "PUT", upload.headers(), upload.expiresAt());
    }
}
