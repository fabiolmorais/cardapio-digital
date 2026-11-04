package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.application.port.in.rodizio.PassagemTable;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PassagemTable", description = "Uma mesa no rodízio de passagem, para os passadores")
record PassagemTableResponse(
        @Schema(requiredMode = REQUIRED) UUID tableId,
        @Schema(requiredMode = REQUIRED, example = "04") String tableLabel,
        @Schema(requiredMode = REQUIRED) String sectorName,
        @Schema(requiredMode = REQUIRED, example = "Churrasco") String rodizioName,
        @Schema(requiredMode = REQUIRED) int people,
        @Schema(requiredMode = REQUIRED, description = "SERVING: cartão verde. PAUSED: vermelho")
        TabRodizioStatus status,
        @Schema(requiredMode = REQUIRED, description = "A última virada do cartão") Instant statusChangedAt
) {

    static PassagemTableResponse from(PassagemTable t) {
        return new PassagemTableResponse(t.tableId().value(), t.tableLabel(), t.sectorName(), t.rodizioName(),
                t.people(), t.status(), t.statusChangedAt());
    }
}
