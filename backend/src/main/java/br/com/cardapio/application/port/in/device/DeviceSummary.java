package br.com.cardapio.application.port.in.device;

import br.com.cardapio.domain.device.model.Device;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.DeviceType;

import java.time.Instant;

/**
 * O que o painel mostra de um aparelho. lastSeenAt vazio: o gerente vinculou, mas o aparelho
 * ainda não buscou o token. O token (nem o hash) nunca sai daqui.
 */
public record DeviceSummary(DeviceId id, String name, DeviceType type, Instant linkedAt, Instant lastSeenAt) {

    public static DeviceSummary from(Device device) {
        return new DeviceSummary(device.id(), device.name(), device.type(), device.createdAt(), device.lastSeenAt());
    }
}
