package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.application.port.in.rodizio.RodizioQuote;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "RodizioQuote", description = "Um rodízio com o preço de cada faixa agora, no relógio do restaurante")
record RodizioQuoteResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) RodizioType type,
        @Schema(requiredMode = REQUIRED) String description,
        @Schema(requiredMode = REQUIRED) List<Tier> tiers
) {

    @Schema(name = "RodizioQuoteTier")
    record Tier(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) long priceCents) {
    }

    static RodizioQuoteResponse from(RodizioQuote q) {
        return new RodizioQuoteResponse(q.id().value(), q.name(), q.type(), q.description(),
                q.tiers().stream().map(t -> new Tier(t.name(), t.price().cents())).toList());
    }
}
