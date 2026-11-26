package br.com.cardapio.adapter.in.web.floor;

import br.com.cardapio.application.port.in.floor.CreateTableCommand;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.floor.model.TableKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "CreateTableRequest")
record CreateTableRequest(
        @Schema(requiredMode = REQUIRED, example = "04", description = "O rótulo da mesa (até 10) ou o nome do balcão (até 30)")
        @NotBlank @Size(max = 30) String label,
        @Schema(requiredMode = REQUIRED) @NotNull UUID sectorId,
        @Schema(description = "Mesa (padrão) ou balcão (ADR 0015): sem QR Code, várias comandas, fora do limite de mesas")
        TableKind kind
) {
    CreateTableCommand toCommand(EstablishmentId establishmentId) {
        return new CreateTableCommand(establishmentId, new SectorId(sectorId), label, kind);
    }
}
