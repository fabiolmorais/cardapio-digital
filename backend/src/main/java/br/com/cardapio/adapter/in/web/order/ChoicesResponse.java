package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.menu.model.ItemChoices;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O tamanho e as escolhas de um item do pedido, como foram copiados na hora (ADR 0010). */
@Schema(name = "ItemChoices")
record ChoicesResponse(
        @Schema(description = "O tamanho escolhido; null sem tamanhos", example = "Grande") String variantName,
        @Schema(requiredMode = REQUIRED, description = "As escolhas, na ordem dos grupos; vazio sem opções")
        List<Option> options
) {

    @Schema(name = "ItemChoice")
    record Option(
            @Schema(requiredMode = REQUIRED, example = "Escolha os sabores") String groupTitle,
            @Schema(requiredMode = REQUIRED, example = "Calabresa") String name,
            @Schema(requiredMode = REQUIRED, description = "Quantas vezes (\"2× bacon\")") int quantity,
            @Schema(requiredMode = REQUIRED,
                    description = "Parte de um item dividido: a tela mostra a fração (½, ⅓, ¼) pelo número de partes")
            boolean splitsItem
    ) {
    }

    static ChoicesResponse from(ItemChoices choices) {
        return new ChoicesResponse(choices.variantName(), choices.options().stream()
                .map(o -> new Option(o.groupTitle(), o.name(), o.quantity(), o.splitsItem())).toList());
    }
}
