package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupPrices;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.shared.model.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** A grade de preços de um grupo de opções: o preço de cada opção em cada tamanho dos produtos que o usam. */
final class OptionGroupPricesDtos {

    private OptionGroupPricesDtos() {
    }

    @Schema(name = "OptionGroupPrices", description = "As colunas (tamanhos dos produtos que usam o grupo) e os "
            + "preços preenchidos. Em branco, vale o preço da opção. Preços em centavos")
    record Response(
            @Schema(requiredMode = REQUIRED) List<Column> columns,
            @Schema(requiredMode = REQUIRED) List<Price> prices
    ) {
        static Response from(OptionGroupPrices grid) {
            return new Response(
                    grid.columns().stream().map(c -> new Column(c.productId().value(), c.productName(),
                            c.variantId() == null ? null : c.variantId().value(), c.variantName())).toList(),
                    grid.prices().stream().map(p -> new Price(p.productId().value(),
                            p.variantId() == null ? null : p.variantId().value(), p.optionId().value(),
                            p.price().cents())).toList());
        }
    }

    @Schema(name = "OptionGroupPriceColumn", description = "Um tamanho de um produto, ou o produto sem tamanhos")
    record Column(
            @Schema(requiredMode = REQUIRED) UUID productId,
            @Schema(requiredMode = REQUIRED, example = "Pizza") String productName,
            @Schema(description = "Vazio: o produto sem tamanhos") UUID variantId,
            @Schema(description = "Vazio: o produto sem tamanhos", example = "Grande") String variantName
    ) {
    }

    @Schema(name = "OptionGroupPrice", description = "O preço de uma opção numa coluna da grade")
    record Price(
            @Schema(requiredMode = REQUIRED) @NotNull UUID productId,
            @Schema(description = "Vazio: o produto sem tamanhos") UUID variantId,
            @Schema(requiredMode = REQUIRED) @NotNull UUID optionId,
            @Schema(requiredMode = REQUIRED, example = "6800") @Min(0) @Max(9_999_999) long priceCents
    ) {
        OptionGroupPrices.Price toPrice() {
            return new OptionGroupPrices.Price(new ProductId(productId),
                    variantId == null ? null : new ProductVariantId(variantId), new OptionItemId(optionId),
                    Money.ofCents(priceCents));
        }
    }

    @Schema(name = "OptionGroupPricesRequest", description = "A grade inteira: o que não vier fica em branco")
    record Request(
            @Schema(requiredMode = REQUIRED) @NotNull @Size(max = 5000) List<@NotNull @Valid Price> prices
    ) {
    }
}
