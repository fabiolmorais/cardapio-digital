package br.com.cardapio.adapter.in.web.presence;

import br.com.cardapio.application.port.in.presence.PresenceStatus;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Presence")
record PresenceResponse(
        @Schema(requiredMode = REQUIRED, description = "Pode chamar o garçom e pedir a conta") boolean present,
        @Schema(description = "Como a presença foi provada; vazio enquanto não foi") PresenceMethod method,
        @Schema(requiredMode = REQUIRED, description = "O restaurante marcou o local: vale oferecer a localização")
        boolean locationEnabled,
        @Schema(description = "O nome do Wi-Fi do salão, para sugerir ao cliente") String wifiName
) {
    static PresenceResponse from(PresenceStatus s) {
        return new PresenceResponse(s.present(), s.method(), s.locationEnabled(), s.wifiName());
    }
}
