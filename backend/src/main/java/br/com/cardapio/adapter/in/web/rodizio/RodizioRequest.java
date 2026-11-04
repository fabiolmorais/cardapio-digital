package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.application.port.in.rodizio.RodizioForm;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.rodizio.model.PriceTier;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TimedPrice;
import br.com.cardapio.domain.shared.model.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O rodízio inteiro, na criação e na edição. Preços em centavos; horários no relógio do restaurante. */
@Schema(name = "RodizioRequest")
record RodizioRequest(
        @Schema(requiredMode = REQUIRED, example = "Rodízio Japonês") @NotBlank @Size(max = 60) String name,
        @Schema(requiredMode = REQUIRED) @NotNull RodizioType type,
        @Schema(description = "O que está incluído, para o cliente") @Size(max = 280) String description,
        @Schema(requiredMode = REQUIRED) @NotNull @Valid Rules rules,
        @Schema(requiredMode = REQUIRED) @NotEmpty @Size(max = 6) List<@NotNull @Valid Tier> tiers,
        @Schema(requiredMode = REQUIRED, description = "Os produtos incluídos") @NotNull @Size(max = 500)
        Set<@NotNull UUID> productIds
) {

    @Schema(name = "RodizioRules", description = "As regras opcionais: vazias, não valem (o padrão)")
    record Rules(
            @Schema(description = "Unidades de cada item numa rodada, por pessoa") @Min(1) @Max(50)
            Integer maxPerItemPerRound,
            @Schema(description = "Itens não entregues por pessoa antes de outra rodada") @Min(1) @Max(100)
            Integer maxPendingPerGuest,
            @Schema(description = "Minutos entre uma rodada e a seguinte") @Min(1) @Max(120)
            Integer minRoundIntervalMinutes,
            @Schema(description = "Duração do rodízio, em minutos") @Min(15) @Max(600) Integer timeLimitMinutes,
            @Schema(description = "O aviso de taxa de desperdício") @Size(max = 200) String wasteNotice
    ) {
    }

    @Schema(name = "RodizioTierRequest")
    record Tier(
            @Schema(requiredMode = REQUIRED, example = "Adulto") @NotBlank @Size(max = 40) String name,
            @Schema(requiredMode = REQUIRED, example = "12990") @NotNull @PositiveOrZero Long defaultPriceCents,
            @Schema(requiredMode = REQUIRED, description = "Os preços especiais por dia e horário")
            @NotNull @Size(max = 8) List<@NotNull @Valid Special> prices
    ) {
    }

    @Schema(name = "RodizioSpecialPriceRequest")
    record Special(
            @Schema(requiredMode = REQUIRED, example = "[\"FRIDAY\", \"SATURDAY\"]") @NotEmpty Set<@NotNull DayOfWeek> days,
            @Schema(requiredMode = REQUIRED, type = "string", example = "18:00") @NotNull LocalTime startsAt,
            @Schema(requiredMode = REQUIRED, type = "string", example = "23:59") @NotNull LocalTime endsAt,
            @Schema(requiredMode = REQUIRED, example = "14990") @NotNull @PositiveOrZero Long priceCents
    ) {
    }

    RodizioForm toForm() {
        return new RodizioForm(name, type, description,
                new RodizioRules(rules.maxPerItemPerRound(), rules.maxPendingPerGuest(),
                        rules.minRoundIntervalMinutes(), rules.timeLimitMinutes(), rules.wasteNotice()),
                tiers.stream().map(t -> new PriceTier(t.name(), Money.ofCents(t.defaultPriceCents()),
                        t.prices().stream().map(p -> new TimedPrice(p.days(), p.startsAt(), p.endsAt(),
                                Money.ofCents(p.priceCents()))).toList())).toList(),
                productIds.stream().map(ProductId::new).collect(Collectors.toSet()));
    }
}
