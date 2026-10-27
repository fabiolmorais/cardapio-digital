package br.com.cardapio.domain.device.model;

import java.util.Objects;
import java.util.UUID;

public record PairingId(UUID value) {

    public PairingId {
        Objects.requireNonNull(value, "id do pedido de vínculo é obrigatório");
    }

    public static PairingId newId() {
        return new PairingId(UUID.randomUUID());
    }
}
