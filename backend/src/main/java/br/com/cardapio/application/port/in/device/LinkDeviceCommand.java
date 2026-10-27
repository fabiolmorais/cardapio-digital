package br.com.cardapio.application.port.in.device;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.DeviceType;

import java.util.Objects;

/** @param establishmentId restaurante de quem está vinculando (vem do token, nunca do corpo) */
public record LinkDeviceCommand(EstablishmentId establishmentId, String code, String name, DeviceType type) {

    public LinkDeviceCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(type);
    }
}
