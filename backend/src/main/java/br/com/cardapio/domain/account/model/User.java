package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.model.Email;

import java.util.Objects;

/**
 * Pessoa que entra no sistema com e-mail e senha. O papel dela fica em {@link Membership}.
 */
public record User(
        UserId id,
        String name,
        Email email,
        PasswordHash passwordHash,
        boolean active
) {

    public User {
        Objects.requireNonNull(id);
        Objects.requireNonNull(email);
        Objects.requireNonNull(passwordHash);
        name = Names.required(name, "Informe o seu nome");
    }

    public static User create(String name, Email email, PasswordHash passwordHash) {
        return new User(UserId.newId(), name, email, passwordHash, true);
    }
}
