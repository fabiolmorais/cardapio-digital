package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.device.ClaimResult;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ClaimPairingResponse", description = "PENDING: pergunte de novo em alguns segundos. "
        + "LINKED: o token foi para o cookie device_token.")
record ClaimPairingResponse(
        @Schema(requiredMode = REQUIRED) Status status,
        @Schema(description = "Só quando LINKED") DeviceResponse device,
        @Schema(description = "Só quando LINKED", example = "Sakura Sushi") String establishmentName
) {
    enum Status { PENDING, LINKED }

    static ClaimPairingResponse from(ClaimResult result) {
        return switch (result) {
            case ClaimResult.Pending pending -> new ClaimPairingResponse(Status.PENDING, null, null);
            case ClaimResult.Linked linked -> new ClaimPairingResponse(Status.LINKED,
                    DeviceResponse.from(linked.device()), linked.establishmentName());
        };
    }
}
