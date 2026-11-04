package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.domain.rodizio.model.Rodizio;
import br.com.cardapio.domain.rodizio.model.RodizioRules;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Rodizio")
record RodizioResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) RodizioType type,
        @Schema(requiredMode = REQUIRED) String description,
        @Schema(requiredMode = REQUIRED) Rules rules,
        @Schema(requiredMode = REQUIRED) List<Tier> tiers,
        @Schema(requiredMode = REQUIRED) List<UUID> productIds
) {

    @Schema(name = "RodizioRulesView")
    record Rules(Integer maxPerItemPerRound, Integer maxPendingPerGuest, Integer minRoundIntervalMinutes,
                 Integer timeLimitMinutes, String wasteNotice) {
    }

    @Schema(name = "RodizioTier")
    record Tier(
            @Schema(requiredMode = REQUIRED) String name,
            @Schema(requiredMode = REQUIRED) long defaultPriceCents,
            @Schema(requiredMode = REQUIRED) List<Special> prices
    ) {
    }

    @Schema(name = "RodizioSpecialPrice")
    record Special(
            @Schema(requiredMode = REQUIRED) Set<DayOfWeek> days,
            @Schema(requiredMode = REQUIRED, type = "string", example = "18:00") LocalTime startsAt,
            @Schema(requiredMode = REQUIRED, type = "string", example = "23:59") LocalTime endsAt,
            @Schema(requiredMode = REQUIRED) long priceCents
    ) {
    }

    static RodizioResponse from(Rodizio r) {
        RodizioRules rules = r.rules();
        return new RodizioResponse(r.id().value(), r.name(), r.type(), r.description(),
                new Rules(rules.maxPerItemPerRound(), rules.maxPendingPerGuest(), rules.minRoundIntervalMinutes(),
                        rules.timeLimitMinutes(), rules.wasteNotice()),
                r.tiers().stream().map(t -> new Tier(t.name(), t.defaultPrice().cents(),
                        t.exceptions().stream().map(e -> new Special(e.days(), e.startsAt(), e.endsAt(),
                                e.price().cents())).toList())).toList(),
                r.productIds().stream().map(p -> p.value()).sorted().toList());
    }
}
