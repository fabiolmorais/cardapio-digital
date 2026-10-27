package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.device.LinkDeviceCommand;
import br.com.cardapio.domain.device.model.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "LinkDeviceRequest", description = "O gerente digita o código que aparece no aparelho novo")
record LinkDeviceRequest(
        @Schema(requiredMode = REQUIRED, example = "482719") @NotBlank @Pattern(regexp = "\\d{6}") String code,
        @Schema(requiredMode = REQUIRED, example = "Celular do salão") @NotBlank @Size(max = 60) String name,
        @Schema(requiredMode = REQUIRED) @NotNull DeviceType type
) {
    LinkDeviceCommand toCommand(AuthenticatedUser user) {
        return new LinkDeviceCommand(user.establishmentId(), code, name, type);
    }
}
