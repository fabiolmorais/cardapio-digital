package br.com.cardapio.adapter.in.web.presence;

import br.com.cardapio.application.port.in.presence.PresenceSettings;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PresenceRule")
record PresenceRuleResponse(
        @Schema(requiredMode = REQUIRED, description = "As faixas, sempre com o tamanho (\"189.45.212.18/32\")")
        List<String> networks,
        String wifiName,
        PresenceRuleRequest.Location location,
        @Schema(description = "A rede de onde você está agora, para o botão \"Usar o IP desta rede\"")
        String currentNetwork,
        @Schema(requiredMode = REQUIRED, description = "Quanto tempo sem uso a sessão da mesa aguenta, em minutos")
        int sessionIdleMinutes
) {
    static PresenceRuleResponse from(PresenceSettings s) {
        var location = s.location() == null ? null : new PresenceRuleRequest.Location(s.location().latitude(),
                s.location().longitude(), s.location().radiusMeters());
        return new PresenceRuleResponse(s.networks(), s.wifiName(), location, s.currentNetwork(),
                s.sessionIdleMinutes());
    }
}
