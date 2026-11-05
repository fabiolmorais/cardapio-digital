package br.com.cardapio.adapter.in.web.push;

import br.com.cardapio.application.port.in.push.PushSettings;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PushSettings")
record PushSettingsResponse(
        @Schema(requiredMode = REQUIRED, description = "false: o servidor está sem as chaves VAPID") boolean enabled,
        @Schema(description = "A chave pública VAPID (base64url), para o navegador se inscrever") String publicKey
) {

    static PushSettingsResponse from(PushSettings s) {
        return new PushSettingsResponse(s.enabled(), s.publicKey());
    }
}
