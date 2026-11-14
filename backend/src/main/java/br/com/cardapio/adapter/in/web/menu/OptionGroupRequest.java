package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.OptionGroupForm;
import br.com.cardapio.domain.menu.model.OptionGroupRules;
import br.com.cardapio.domain.menu.model.OptionItemId;
import br.com.cardapio.domain.menu.model.OptionPricing;
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

/** O formulário do grupo de opções, com as opções na ordem em que o cliente vê. */
@Schema(name = "OptionGroupRequest")
record OptionGroupRequest(
        @Schema(description = "O nome na lista da gestão; vazio: usa a pergunta", example = "Sabores da pizza")
        @Size(max = 60) String name,
        @Schema(requiredMode = REQUIRED, example = "Escolha os sabores") @NotBlank @Size(max = 60) String title,
        @Schema(requiredMode = REQUIRED, description = "0 = opcional") @Min(0) @Max(20) int minChoices,
        @Schema(requiredMode = REQUIRED) @Min(1) @Max(20) int maxChoices,
        @Schema(requiredMode = REQUIRED, description = "1 = sem repetir") @Min(1) @Max(20) int maxPerOption,
        @Schema(requiredMode = REQUIRED, description = "As escolhas são partes do mesmo item (meio a meio)")
        boolean splitsItem,
        @Schema(requiredMode = REQUIRED) @NotNull OptionPricing pricing,
        @Schema(requiredMode = REQUIRED, description = "As primeiras N escolhas não cobram (só ao somar)")
        @Min(0) @Max(20) int freeQuantity,
        @Schema(requiredMode = REQUIRED) @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid Item> items
) {

    @Schema(name = "OptionItemRequest")
    record Item(
            @Schema(description = "Vazio numa opção nova; o id de uma opção que já existe para editá-la") UUID id,
            @Schema(requiredMode = REQUIRED, example = "Calabresa") @NotBlank @Size(max = 60) String name,
            @Schema(requiredMode = REQUIRED, description = "Em centavos (pode ser zero)", example = "0")
            @Min(0) @Max(9_999_999) long priceCents,
            @Schema(requiredMode = REQUIRED, description = "false: esgotada") boolean available
    ) {
    }

    OptionGroupForm toForm() {
        return new OptionGroupForm(name, title,
                new OptionGroupRules(minChoices, maxChoices, maxPerOption, splitsItem, pricing, freeQuantity),
                items.stream().map(i -> new OptionGroupForm.Item(i.id() == null ? null : new OptionItemId(i.id()),
                        i.name(), Money.ofCents(i.priceCents()), i.available())).toList());
    }
}
