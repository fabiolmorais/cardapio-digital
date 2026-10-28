package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.TableSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "DiningTable", description = "Mesa do salão")
record TableResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) UUID sectorId,
        @Schema(requiredMode = REQUIRED, example = "04") String label,
        @Schema(requiredMode = REQUIRED, description = "Vai na URL do QR Code: /m/{publicToken}") String publicToken,
        @Schema(requiredMode = REQUIRED, description = "Quando o QR atual foi gerado") Instant tokenIssuedAt,
        @Schema(requiredMode = REQUIRED) boolean active
) {
    static TableResponse from(TableSummary t) {
        return new TableResponse(t.id().value(), t.sectorId().value(), t.label(), t.publicToken(), t.tokenIssuedAt(),
                t.active());
    }
}
