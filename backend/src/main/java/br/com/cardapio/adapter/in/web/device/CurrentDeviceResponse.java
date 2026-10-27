package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.device.CurrentDevice;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "CurrentDevice", description = "O aparelho que está chamando e o restaurante dele")
record CurrentDeviceResponse(
        @Schema(requiredMode = REQUIRED) DeviceResponse device,
        @Schema(requiredMode = REQUIRED, example = "Sakura Sushi") String establishmentName
) {
    static CurrentDeviceResponse from(CurrentDevice current) {
        return new CurrentDeviceResponse(DeviceResponse.from(current.device()), current.establishmentName());
    }
}
