package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.MenuCategorySummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "MenuCategory", description = "Categoria do cardápio")
record MenuCategoryResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "Entradas") String name,
        @Schema(requiredMode = REQUIRED) int position,
        @Schema(requiredMode = REQUIRED, description = "Quantos produtos ativos a categoria tem") long activeProducts
) {
    static MenuCategoryResponse from(MenuCategorySummary c) {
        return new MenuCategoryResponse(c.id().value(), c.name(), c.position(), c.activeProducts());
    }
}
