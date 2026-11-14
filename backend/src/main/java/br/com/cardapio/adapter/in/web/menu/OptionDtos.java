package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupSummary;
import br.com.cardapio.domain.menu.model.ChoiceLimit;
import br.com.cardapio.domain.menu.model.OptionGroup;
import br.com.cardapio.domain.menu.model.OptionItem;
import br.com.cardapio.domain.menu.model.OptionPricing;
import br.com.cardapio.domain.menu.model.ProductOptions;
import br.com.cardapio.domain.menu.model.ProductVariant;
import br.com.cardapio.domain.menu.model.ProductVariantId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** As respostas das opções dos produtos (ADR 0010), usadas na gestão e no cardápio da mesa. */
final class OptionDtos {

    private OptionDtos() {
    }

    @Schema(name = "OptionItem", description = "Uma opção de um grupo. Preço em centavos (pode ser zero)")
    record Item(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED, example = "Calabresa") String name,
            @Schema(requiredMode = REQUIRED, example = "500") long priceCents,
            @Schema(requiredMode = REQUIRED, description = "false: esgotada") boolean available
    ) {
        static Item from(OptionItem i) {
            return new Item(i.id().value(), i.name(), i.price().cents(), i.available());
        }
    }

    @Schema(name = "OptionGroup", description = "Um grupo de opções da biblioteca do restaurante")
    record Group(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED, description = "O nome na lista da gestão", example = "Sabores da pizza")
            String name,
            @Schema(requiredMode = REQUIRED, description = "O que o cliente vê", example = "Escolha os sabores")
            String title,
            @Schema(requiredMode = REQUIRED, description = "0 = opcional") int minChoices,
            @Schema(requiredMode = REQUIRED) int maxChoices,
            @Schema(requiredMode = REQUIRED, description = "Vezes que a mesma opção pode ser escolhida") int maxPerOption,
            @Schema(requiredMode = REQUIRED, description = "As escolhas são partes do mesmo item (meio a meio)")
            boolean splitsItem,
            @Schema(requiredMode = REQUIRED) OptionPricing pricing,
            @Schema(requiredMode = REQUIRED, description = "As primeiras N escolhas não cobram (só ao somar)")
            int freeQuantity,
            @Schema(description = "Em quantos produtos ativos o grupo está; null dentro de um produto")
            Long productCount,
            @Schema(requiredMode = REQUIRED, description = "As opções ativas, na ordem") List<Item> items
    ) {
        static Group from(OptionGroupSummary g) {
            return from(g.group(), g.items(), g.productCount());
        }

        static Group from(OptionGroup g, List<OptionItem> items, Long productCount) {
            var r = g.rules();
            return new Group(g.id().value(), g.name(), g.title(), r.minChoices(), r.maxChoices(), r.maxPerOption(),
                    r.splitsItem(), r.pricing(), r.freeQuantity(), productCount, items.stream().map(Item::from).toList());
        }
    }

    @Schema(name = "ProductVariant", description = "Um tamanho do produto. Preços em centavos")
    record Variant(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED, example = "Grande") String name,
            @Schema(requiredMode = REQUIRED, example = "5990") long priceCents,
            @Schema(description = "null quando não há promoção") Long promoPriceCents,
            @Schema(requiredMode = REQUIRED, description = "false: esgotado") boolean available,
            @Schema(requiredMode = REQUIRED, description = "É o tamanho que vale no rodízio") boolean rodizioDefault
    ) {
        static Variant from(ProductVariant v) {
            return new Variant(v.id().value(), v.name(), v.price().cents(),
                    v.promoPrice() == null ? null : v.promoPrice().cents(), v.available(), v.rodizioDefault());
        }
    }

    @Schema(name = "OptionGroupLimit", description = "O mínimo e o máximo de um grupo num tamanho")
    record Limit(
            @Schema(requiredMode = REQUIRED) UUID variantId,
            @Schema(requiredMode = REQUIRED) int minChoices,
            @Schema(requiredMode = REQUIRED) int maxChoices
    ) {
        static List<Limit> from(Map<ProductVariantId, ChoiceLimit> limits) {
            return limits.entrySet().stream()
                    .map(e -> new Limit(e.getKey().value(), e.getValue().minChoices(), e.getValue().maxChoices()))
                    .toList();
        }
    }

    @Schema(name = "OptionPrice", description = "O preço de uma opção num tamanho, em centavos")
    record Price(
            @Schema(requiredMode = REQUIRED) UUID optionId,
            @Schema(requiredMode = REQUIRED) UUID variantId,
            @Schema(requiredMode = REQUIRED) long priceCents
    ) {
        static List<Price> from(ProductOptions options) {
            return options.prices().entrySet().stream()
                    .filter(e -> options.variants().stream().anyMatch(v -> v.id().equals(e.getKey().variantId())))
                    .map(e -> new Price(e.getKey().optionId().value(), e.getKey().variantId().value(),
                            e.getValue().cents()))
                    .toList();
        }
    }
}
