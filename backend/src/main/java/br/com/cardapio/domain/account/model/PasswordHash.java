package br.com.cardapio.domain.account.model;

import java.util.Objects;

/**
 * Senha já protegida (hash). O domínio nunca guarda a senha original.
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "hash da senha é obrigatório");
        if (value.isBlank()) {
            throw new IllegalArgumentException("hash da senha não pode ser vazio");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[protegido]";
    }
}
