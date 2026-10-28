package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.SectorSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Sector", description = "Setor do salão")
record SectorResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "Salão principal") String name,
        @Schema(requiredMode = REQUIRED, description = "Ordem de exibição, a partir de 0") int position,
        @Schema(requiredMode = REQUIRED, description = "Mesas ativas no setor") long activeTables
) {
    static SectorResponse from(SectorSummary s) {
        return new SectorResponse(s.id().value(), s.name(), s.position(), s.activeTables());
    }
}
