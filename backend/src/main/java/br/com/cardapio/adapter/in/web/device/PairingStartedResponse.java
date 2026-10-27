package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.device.PairingStarted;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PairingStarted", description = "Pedido de vínculo aberto pelo aparelho novo")
record PairingStartedResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, description = "Mostrar na tela do aparelho", example = "482719") String code,
        @Schema(requiredMode = REQUIRED, description = "Guardar no aparelho e mandar ao buscar o token; nunca mostrar")
        String secret,
        @Schema(requiredMode = REQUIRED) Instant expiresAt
) {
    static PairingStartedResponse from(PairingStarted p) {
        return new PairingStartedResponse(p.id().value(), p.code().value(), p.secret(), p.expiresAt());
    }

    @Override
    public String toString() {
        return "PairingStartedResponse[id=" + id + "]";
    }
}
