package br.com.cardapio.domain.account.model;

import java.util.Objects;
import java.util.UUID;

public record RefreshTokenId(UUID value) {

    public RefreshTokenId {
        Objects.requireNonNull(value, "id do refresh token é obrigatório");
    }

    public static RefreshTokenId newId() {
        return new RefreshTokenId(UUID.randomUUID());
    }
}
