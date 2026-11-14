package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.OptionSelection;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import br.com.cardapio.domain.menu.model.SelectedOption;
import br.com.cardapio.domain.order.model.OrderLine;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O carrinho: só o produto, a quantidade e a observação. O nome e o preço o back copia do cardápio. */
@Schema(name = "PlaceOrderRequest")
record PlaceOrderRequest(
        @Schema(requiredMode = REQUIRED) @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid Line> items
) {

    @Schema(name = "PlaceOrderLine")
    record Line(
            @Schema(requiredMode = REQUIRED) @NotNull UUID productId,
            @Schema(requiredMode = REQUIRED, example = "2") @Min(1) @Max(20) int quantity,
            @Schema(description = "Observação do item", example = "sem cebolinha") @Size(max = 140) String note,
            @Schema(description = "O tamanho escolhido; vazio se o produto não tem tamanhos") UUID variantId,
            @Schema(description = "As opções escolhidas; vazio se o produto não tem opções")
            @Size(max = 100) List<@NotNull @Valid Choice> options
    ) {
        OrderLine toLine() {
            return new OrderLine(new ProductId(productId), quantity, note, new OptionSelection(
                    variantId == null ? null : new ProductVariantId(variantId),
                    options == null ? List.of() : options.stream()
                            .map(o -> new SelectedOption(new OptionItemId(o.optionId()), o.quantity())).toList()));
        }
    }

    @Schema(name = "PlaceOrderChoice", description = "Uma opção escolhida e quantas vezes")
    record Choice(
            @Schema(requiredMode = REQUIRED) @NotNull UUID optionId,
            @Schema(requiredMode = REQUIRED, example = "1") @Min(1) @Max(20) int quantity
    ) {
    }

    List<OrderLine> toLines() {
        return items.stream().map(Line::toLine).toList();
    }
}
