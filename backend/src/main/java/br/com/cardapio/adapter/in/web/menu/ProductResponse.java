package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.ProductSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Product", description = "Produto do cardápio. Preços em centavos")
record ProductResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) UUID categoryId,
        @Schema(requiredMode = REQUIRED, example = "Hot roll da casa") String name,
        @Schema(requiredMode = REQUIRED, description = "Vazia quando o dono não escreveu") String description,
        @Schema(requiredMode = REQUIRED, example = "2790") long priceCents,
        @Schema(description = "null quando não há promoção", example = "2490") Long promoPriceCents,
        @Schema(requiredMode = REQUIRED) boolean featured,
        @Schema(requiredMode = REQUIRED) boolean available,
        @Schema(requiredMode = REQUIRED) int position,
        @Schema(requiredMode = REQUIRED, description = "false: só do rodízio") boolean aLaCarte,
        @Schema(description = "Quantas pessoas o prato serve; null quando não informado", example = "2") Integer serves,
        @Schema(description = "Fotos da galeria; só na lista (null nos outros casos)") Integer photoCount,
        @Schema(description = "Vídeos da galeria (sem os que falharam); só na lista") Integer videoCount
) {
    static ProductResponse from(ProductSummary p) {
        return new ProductResponse(p.id().value(), p.categoryId().value(), p.name(), p.description(), p.priceCents(),
                p.promoPriceCents(), p.featured(), p.available(), p.position(), p.aLaCarte(), p.serves(), p.photoCount(),
                p.videoCount());
    }
}
