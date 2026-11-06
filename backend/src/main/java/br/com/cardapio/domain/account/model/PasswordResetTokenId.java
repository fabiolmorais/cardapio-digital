package br.com.cardapio.domain.account.model;

import java.util.Objects;
import java.util.UUID;

public record PasswordResetTokenId(UUID value) {

    public PasswordResetTokenId {
        Objects.requireNonNull(value);
    }

    public static PasswordResetTokenId newId() {
        return new PasswordResetTokenId(UUID.randomUUID());
    }
}
