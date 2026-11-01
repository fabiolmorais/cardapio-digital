package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.ProductMediaSummary;
import br.com.cardapio.domain.menu.model.ImageVariant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ProductMedia", description = "Foto do produto. A de posição mais baixa é a capa")
record ProductMediaResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "IMAGE") String type,
        @Schema(requiredMode = REQUIRED) int position,
        @Schema(requiredMode = REQUIRED, description = "Largura do tamanho grande, em pixels") int width,
        @Schema(requiredMode = REQUIRED, description = "Altura do tamanho grande, em pixels") int height,
        @Schema(requiredMode = REQUIRED) Urls urls
) {
    @Schema(name = "ProductMediaUrls", description = "Um endereço por tamanho")
    record Urls(
            @Schema(requiredMode = REQUIRED, description = "Quadrada, 240 px") String thumb,
            @Schema(requiredMode = REQUIRED, description = "Até 720 px no maior lado") String medium,
            @Schema(requiredMode = REQUIRED, description = "Até 1440 px no maior lado") String large
    ) {
    }

    static ProductMediaResponse from(ProductMediaSummary m) {
        return new ProductMediaResponse(m.id().value(), m.type().name(), m.position(), m.width(), m.height(),
                new Urls(m.urls().get(ImageVariant.THUMB), m.urls().get(ImageVariant.MEDIUM),
                        m.urls().get(ImageVariant.LARGE)));
    }
}
