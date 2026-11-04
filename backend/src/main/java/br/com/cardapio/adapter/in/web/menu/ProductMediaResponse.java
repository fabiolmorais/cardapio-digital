package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ProductMedia")
record ProductMediaResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) MediaType type,
        @Schema(requiredMode = REQUIRED) int position,
        @Schema(requiredMode = REQUIRED,
                description = "Largura (da foto grande ou do vídeo); 0 no vídeo em conversão")
        int width,
        @Schema(requiredMode = REQUIRED, description = "Altura, idem") int height,
        @Schema(requiredMode = REQUIRED, description = "Foto: sempre pronta. Vídeo: em conversão, pronto ou falhou")
        MediaStatus status,
        @Schema(description = "Os tamanhos da foto; ausente no vídeo") Urls urls,
        @Schema(description = "O vídeo pronto, para tocar") Video video,
        @Schema(description = "Por que o vídeo não ficou pronto") String failure
) {
    @Schema(name = "ProductMediaUrls", description = "Um endereço por tamanho")
    record Urls(
            @Schema(requiredMode = REQUIRED, description = "Quadrada, 240 px") String thumb,
            @Schema(requiredMode = REQUIRED, description = "Até 720 px no maior lado") String medium,
            @Schema(requiredMode = REQUIRED, description = "Até 1440 px no maior lado") String large
    ) {
    }

    @Schema(name = "ProductMediaVideo")
    record Video(
            @Schema(requiredMode = REQUIRED, description = "O MP4 para tocar") String url,
            @Schema(description = "A capa do vídeo, se o serviço gera uma") String poster,
            @Schema(requiredMode = REQUIRED) int durationSeconds
    ) {
    }

    static ProductMediaResponse from(ProductMediaSummary m) {
        Urls urls = m.urls().isEmpty() ? null : new Urls(m.urls().get(ImageVariant.THUMB),
                m.urls().get(ImageVariant.MEDIUM), m.urls().get(ImageVariant.LARGE));
        Video video = m.videoUrl() == null ? null : new Video(m.videoUrl(), m.posterUrl(), m.durationSeconds());
        return new ProductMediaResponse(m.id().value(), m.type(), m.position(), m.width(), m.height(), m.status(),
                urls, video, m.failure());
    }
}
