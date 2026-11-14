package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.domain.menu.model.ProductOptions;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ProductOptions", description = "Os tamanhos e os grupos de um produto (ADR 0010)")
record ProductOptionsResponse(
        @Schema(requiredMode = REQUIRED) List<OptionDtos.Variant> variants,
        @Schema(requiredMode = REQUIRED, description = "Os grupos ligados, na ordem em que o cliente vê")
        List<Linked> groups,
        @Schema(requiredMode = REQUIRED) List<OptionDtos.Price> prices
) {

    @Schema(name = "ProductOptionGroup")
    record Linked(
            @Schema(requiredMode = REQUIRED) OptionDtos.Group group,
            @Schema(requiredMode = REQUIRED) List<OptionDtos.Limit> limits
    ) {
    }

    static ProductOptionsResponse from(ProductOptions options) {
        return new ProductOptionsResponse(options.variants().stream().map(OptionDtos.Variant::from).toList(),
                options.groups().stream().map(g -> new Linked(OptionDtos.Group.from(g.group(), g.items(), null),
                        OptionDtos.Limit.from(g.limits()))).toList(),
                OptionDtos.Price.from(options));
    }
}
