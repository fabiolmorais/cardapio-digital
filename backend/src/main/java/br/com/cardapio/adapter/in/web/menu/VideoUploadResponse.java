package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.VideoUpload;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "VideoUpload",
        description = "Envie o arquivo pelo protocolo TUS no endpoint, com estes cabeçalhos no POST de criação")
record VideoUploadResponse(
        @Schema(requiredMode = REQUIRED, description = "A mídia já criada, em conversão") UUID mediaId,
        @Schema(requiredMode = REQUIRED, description = "O endereço TUS do serviço de vídeo") String endpoint,
        @Schema(requiredMode = REQUIRED, description = "A autorização do envio deste vídeo")
        Map<String, String> headers,
        @Schema(requiredMode = REQUIRED) Instant expiresAt
) {
    static VideoUploadResponse from(VideoUpload upload) {
        return new VideoUploadResponse(upload.mediaId().value(), upload.endpoint(), upload.headers(),
                upload.expiresAt());
    }
}
