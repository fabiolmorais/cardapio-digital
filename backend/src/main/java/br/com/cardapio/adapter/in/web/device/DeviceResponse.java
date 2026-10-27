package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.domain.device.model.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Device", description = "Aparelho vinculado ao restaurante")
record DeviceResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "Celular do salão") String name,
        @Schema(requiredMode = REQUIRED) DeviceType type,
        @Schema(requiredMode = REQUIRED) Instant linkedAt,
        @Schema(description = "Vazio: o aparelho ainda não buscou o token") Instant lastSeenAt
) {
    static DeviceResponse from(DeviceSummary d) {
        return new DeviceResponse(d.id().value(), d.name(), d.type(), d.linkedAt(), d.lastSeenAt());
    }
}
