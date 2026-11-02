package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.PublicMenu;
import br.com.cardapio.domain.menu.model.ImageVariant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * O cardápio da página da mesa, inteiro, como UM documento (ADR 0006): não é uma lista
 * paginada, e o teto de 500 produtos (Product.MAX_PER_ESTABLISHMENT) mantém o tamanho sob controle.
 */
@Schema(name = "PublicMenu")
record PublicMenuResponse(@Schema(requiredMode = REQUIRED) List<Category> categories) {

    @Schema(name = "PublicMenuCategory")
    record Category(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED, example = "Entradas") String name,
            @Schema(requiredMode = REQUIRED) List<Item> products
    ) {
    }

    @Schema(name = "PublicMenuItem", description = "Preços em centavos")
    record Item(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) String name,
            @Schema(requiredMode = REQUIRED, description = "Vazia quando o restaurante não escreveu") String description,
            @Schema(requiredMode = REQUIRED, example = "1990") long priceCents,
            @Schema(description = "null quando não há promoção", example = "1590") Long promoPriceCents,
            @Schema(requiredMode = REQUIRED) boolean featured,
            @Schema(requiredMode = REQUIRED, description = "false: \"Esgotado hoje\" (aparece, mas não pode ser pedido)") boolean available,
            @Schema(description = "A capa; null quando o produto não tem foto") Photo photo
    ) {
    }

    @Schema(name = "PublicMenuPhoto")
    record Photo(
            @Schema(requiredMode = REQUIRED, description = "Quadrada, 240 px") String thumb,
            @Schema(requiredMode = REQUIRED, description = "Até 720 px") String medium,
            @Schema(requiredMode = REQUIRED, description = "Até 1440 px") String large
    ) {
    }

    static PublicMenuResponse from(PublicMenu menu) {
        return new PublicMenuResponse(menu.categories().stream()
                .map(c -> new Category(c.id().value(), c.name(), c.products().stream()
                        .map(p -> new Item(p.id().value(), p.name(), p.description(), p.priceCents(),
                                p.promoPriceCents(), p.featured(), p.available(),
                                p.photoUrls().isEmpty() ? null : new Photo(p.photoUrls().get(ImageVariant.THUMB),
                                        p.photoUrls().get(ImageVariant.MEDIUM), p.photoUrls().get(ImageVariant.LARGE))))
                        .toList()))
                .toList());
    }
}
