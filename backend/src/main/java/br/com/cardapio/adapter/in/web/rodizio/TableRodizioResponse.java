package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.application.port.in.rodizio.TableRodizio;
import br.com.cardapio.domain.rodizio.model.RodizioType;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** O rodízio da mesa: vai na comanda da equipe e nos pedidos do celular. */
@Schema(name = "TableRodizio")
public record TableRodizioResponse(
        @Schema(requiredMode = REQUIRED) UUID rodizioId,
        @Schema(requiredMode = REQUIRED, example = "Rodízio Japonês") String name,
        @Schema(requiredMode = REQUIRED) RodizioType type,
        @Schema(requiredMode = REQUIRED, description = "Todas as faixas, mesmo as com 0 pessoas") List<Guest> guests,
        @Schema(requiredMode = REQUIRED, description = "Pessoas no rodízio, somando as faixas") int people,
        @Schema(requiredMode = REQUIRED, description = "Pessoas × preço: o que o rodízio soma na conta")
        long totalCents,
        @Schema(requiredMode = REQUIRED) Instant startedAt,
        @Schema(description = "O fim previsto (início + tempo limite); ausente sem tempo limite") Instant endsAt,
        @Schema(requiredMode = REQUIRED, description = "O cartão: SERVING (verde) ou PAUSED (vermelho)")
        TabRodizioStatus status,
        @Schema(requiredMode = REQUIRED, description = "A última virada do cartão") Instant statusChangedAt
) {

    @Schema(name = "TableRodizioGuest")
    public record Guest(
            @Schema(requiredMode = REQUIRED, example = "Adulto") String tierName,
            @Schema(requiredMode = REQUIRED) int quantity,
            @Schema(requiredMode = REQUIRED, description = "O preço copiado na ativação") long unitPriceCents
    ) {
    }

    public static TableRodizioResponse from(TableRodizio r) {
        return new TableRodizioResponse(r.rodizioId().value(), r.name(), r.type(),
                r.guests().stream().map(g -> new Guest(g.tierName(), g.quantity(), g.unitPrice().cents())).toList(),
                r.people(), r.total().cents(), r.startedAt(), r.endsAt(), r.status(), r.statusChangedAt());
    }
}
