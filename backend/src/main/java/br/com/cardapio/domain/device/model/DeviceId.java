package br.com.cardapio.domain.device.model;

import java.util.Objects;
import java.util.UUID;

public record DeviceId(UUID value) {

    public DeviceId {
        Objects.requireNonNull(value, "id do aparelho é obrigatório");
    }

    public static DeviceId newId() {
        return new DeviceId(UUID.randomUUID());
    }
}
