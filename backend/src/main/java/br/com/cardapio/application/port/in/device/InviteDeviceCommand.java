package br.com.cardapio.application.port.in.device;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.device.model.DeviceType;

import java.util.Objects;

/** @param establishmentId restaurante de quem está convidando (vem do token, nunca do corpo) */
public record InviteDeviceCommand(EstablishmentId establishmentId, String name, DeviceType type) {

    public InviteDeviceCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(type);
    }
}
