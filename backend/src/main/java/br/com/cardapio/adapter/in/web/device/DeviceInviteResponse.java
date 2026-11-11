package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.device.DeviceInvite;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "DeviceInvite", description = "O QR Code que vincula o aparelho novo, para mostrar no painel")
record DeviceInviteResponse(
        @Schema(requiredMode = REQUIRED) DeviceResponse device,
        @Schema(requiredMode = REQUIRED, description = "O endereço do QR; leva o segredo do convite, nunca registrar")
        String url,
        @Schema(requiredMode = REQUIRED, description = "O QR Code do endereço, em SVG") String qrSvg,
        @Schema(requiredMode = REQUIRED) Instant expiresAt
) {
    static DeviceInviteResponse from(DeviceInvite invite) {
        return new DeviceInviteResponse(DeviceResponse.from(invite.device()), invite.url(), invite.qrSvg(),
                invite.expiresAt());
    }

    @Override
    public String toString() {
        return "DeviceInviteResponse[device=" + device.id() + "]";
    }
}
