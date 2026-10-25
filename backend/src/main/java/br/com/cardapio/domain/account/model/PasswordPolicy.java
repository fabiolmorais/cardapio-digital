package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.nio.charset.StandardCharsets;

/**
 * Regra da senha do painel. O limite de 72 bytes vem do BCrypt, que ignora o que passa disso.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void check(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new InvalidValueException("A senha precisa ter pelo menos " + MIN_LENGTH + " caracteres");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidValueException("A senha é longa demais");
        }
        if (!rawPassword.matches(".*\\p{L}.*") || !rawPassword.matches(".*\\d.*")) {
            throw new InvalidValueException("A senha precisa ter letras e números");
        }
    }
}
