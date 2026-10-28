package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.UpdateTableCommand;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(name = "UpdateTableRequest", description = "Só os campos enviados mudam")
record UpdateTableRequest(
        @Schema(example = "04") @Size(min = 1, max = 10) String label,
        @Schema(description = "Mover para outro setor") UUID sectorId,
        @Schema(description = "Ativar ou desativar a mesa") Boolean active
) {
    UpdateTableCommand toCommand(EstablishmentId establishmentId, DiningTableId tableId) {
        return new UpdateTableCommand(establishmentId, tableId, label,
                sectorId == null ? null : new SectorId(sectorId), active);
    }
}
