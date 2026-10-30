package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.ProductForm;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O formulário do produto, para criar (POST) e para editar (PUT). Preços em centavos. */
@Schema(name = "ProductRequest")
record ProductRequest(
        @Schema(requiredMode = REQUIRED) @NotNull UUID categoryId,
        @Schema(requiredMode = REQUIRED, example = "Hot roll da casa") @NotBlank @Size(max = 80) String name,
        @Schema(example = "Empanado na hora, com salmão e cream cheese. 4 peças.") @Size(max = 300) String description,
        @Schema(requiredMode = REQUIRED, description = "Em centavos: 2790 = R$ 27,90", example = "2790")
        @NotNull @PositiveOrZero @Max(9_999_999) Long priceCents,
        @Schema(description = "Em centavos, menor que o preço. Vazio: sem promoção", example = "2490")
        @Positive @Max(9_999_999) Long promoPriceCents,
        @Schema(requiredMode = REQUIRED) @NotNull Boolean featured,
        @Schema(requiredMode = REQUIRED, description = "false: aparece como \"Esgotado hoje\"") @NotNull Boolean available
) {
    ProductForm toForm() {
        return new ProductForm(new MenuCategoryId(categoryId), name, description, priceCents, promoPriceCents,
                featured, available);
    }
}
