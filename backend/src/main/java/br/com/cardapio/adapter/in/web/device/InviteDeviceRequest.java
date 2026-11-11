package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.device.InviteDeviceCommand;
import br.com.cardapio.domain.device.model.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "InviteDeviceRequest", description = "O aparelho que o gerente vai vincular por QR Code")
record InviteDeviceRequest(
        @Schema(requiredMode = REQUIRED, example = "Celular do salão") @NotBlank @Size(max = 60) String name,
        @Schema(requiredMode = REQUIRED) @NotNull DeviceType type
) {
    InviteDeviceCommand toCommand(AuthenticatedUser user) {
        return new InviteDeviceCommand(user.establishmentId(), name, type);
    }
}
