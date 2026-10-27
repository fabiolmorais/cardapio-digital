package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.CreateTableCommand;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "CreateTableRequest")
record CreateTableRequest(
        @Schema(requiredMode = REQUIRED, example = "04") @NotBlank @Size(max = 10) String label,
        @Schema(requiredMode = REQUIRED) @NotNull UUID sectorId
) {
    CreateTableCommand toCommand(EstablishmentId establishmentId) {
        return new CreateTableCommand(establishmentId, new SectorId(sectorId), label);
    }
}
