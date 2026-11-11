package br.com.cardapio.adapter.in.web.tablesession;

import br.com.cardapio.application.port.in.tablesession.BlockedPhone;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "BlockedPhone", description = "Celular bloqueado pela equipe, com a sessão ainda aberta")
record BlockedPhoneResponse(
        @Schema(requiredMode = REQUIRED, description = "A sessão do celular (o cliente é anônimo)") UUID id,
        @Schema(requiredMode = REQUIRED) UUID tableId,
        @Schema(requiredMode = REQUIRED, example = "04") String tableLabel,
        @Schema(requiredMode = REQUIRED, example = "Salão principal") String sectorName,
        @Schema(requiredMode = REQUIRED) Instant blockedAt,
        @Schema(description = "Quem bloqueou", example = "Carlos Souza") String blockedByName
) {
    static BlockedPhoneResponse from(BlockedPhone p) {
        return new BlockedPhoneResponse(p.sessionId().value(), p.tableId().value(), p.tableLabel(), p.sectorName(),
                p.blockedAt(), p.blockedByName());
    }
}
