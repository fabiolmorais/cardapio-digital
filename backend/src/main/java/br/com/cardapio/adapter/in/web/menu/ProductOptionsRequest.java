package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.ProductOptionsForm;
import br.com.cardapio.domain.menu.model.ChoiceLimit;
import br.com.cardapio.domain.menu.model.OptionGroupId;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Os tamanhos e os grupos de um produto. Os limites e os preços apontam para o tamanho pela
 * posição na lista {@code variants} (o tamanho novo ainda não tem id).
 */
@Schema(name = "ProductOptionsRequest")
record ProductOptionsRequest(
        @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 10) List<@NotNull @Valid Variant> variants,
        @Schema(requiredMode = REQUIRED, description = "Os grupos ligados, na ordem em que o cliente vê")
        @NotNull @Size(max = 10) List<@NotNull @Valid Group> groups,
        @Schema(requiredMode = REQUIRED, description = "Preços de opções num tamanho (quando mudam)")
        @NotNull @Size(max = 500) List<@NotNull @Valid Price> prices
) {

    @Schema(name = "ProductVariantRequest")
    record Variant(
            @Schema(description = "Vazio num tamanho novo; o id de um que já existe para editá-lo") UUID id,
            @Schema(requiredMode = REQUIRED, example = "Grande") @NotBlank @Size(max = 30) String name,
            @Schema(requiredMode = REQUIRED, example = "5990") @Min(0) @Max(9_999_999) long priceCents,
            @Schema(description = "Vazio: sem promoção") @Min(1) @Max(9_999_999) Long promoPriceCents,
            @Schema(requiredMode = REQUIRED) boolean available,
            @Schema(requiredMode = REQUIRED, description = "É o tamanho que vale no rodízio (no máximo um)")
            boolean rodizioDefault
    ) {
    }

    @Schema(name = "ProductOptionGroupRequest")
    record Group(
            @Schema(requiredMode = REQUIRED) @NotNull UUID groupId,
            @Schema(requiredMode = REQUIRED, description = "Mínimo e máximo do grupo em cada tamanho (quando mudam)")
            @NotNull @Size(max = 10) List<@NotNull @Valid Limit> limits
    ) {
    }

    @Schema(name = "OptionGroupLimitRequest")
    record Limit(
            @Schema(requiredMode = REQUIRED, description = "A posição do tamanho na lista variants") @Min(0) @Max(9) int variant,
            @Schema(requiredMode = REQUIRED) @Min(0) @Max(20) int minChoices,
            @Schema(requiredMode = REQUIRED) @Min(1) @Max(20) int maxChoices
    ) {
    }

    @Schema(name = "OptionPriceRequest")
    record Price(
            @Schema(requiredMode = REQUIRED) @NotNull UUID optionId,
            @Schema(requiredMode = REQUIRED, description = "A posição do tamanho na lista variants") @Min(0) @Max(9) int variant,
            @Schema(requiredMode = REQUIRED) @Min(0) @Max(9_999_999) long priceCents
    ) {
    }

    ProductOptionsForm toForm() {
        return new ProductOptionsForm(
                variants.stream().map(v -> new ProductOptionsForm.Variant(
                        v.id() == null ? null : new ProductVariantId(v.id()), v.name(), Money.ofCents(v.priceCents()),
                        v.promoPriceCents() == null ? null : Money.ofCents(v.promoPriceCents()), v.available(),
                        v.rodizioDefault())).toList(),
                groups.stream().map(g -> new ProductOptionsForm.Group(new OptionGroupId(g.groupId()),
                        g.limits().stream().map(l -> new ProductOptionsForm.Limit(l.variant(),
                                new ChoiceLimit(l.minChoices(), l.maxChoices()))).toList())).toList(),
                prices.stream().map(p -> new ProductOptionsForm.Price(new OptionItemId(p.optionId()), p.variant(),
                        Money.ofCents(p.priceCents()))).toList());
    }
}
