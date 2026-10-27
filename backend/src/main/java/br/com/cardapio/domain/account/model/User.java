package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.model.Email;

import java.util.Objects;

/**
 * Pessoa que trabalha num ou mais restaurantes. O papel dela fica em {@link Membership}.
 *
 * Quem entra no painel (dono) tem e-mail e senha. Garçom e cozinha não têm nenhum dos dois:
 * entram por PIN num aparelho vinculado. Por isso e-mail e senha são opcionais, mas andam juntos.
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
        name = Names.required(name, "Informe o seu nome");
        if ((email == null) != (passwordHash == null)) {
            throw new IllegalArgumentException("e-mail e senha andam juntos: os dois ou nenhum");
        }
    }

    /** Dono que se cadastra: entra no painel com e-mail e senha. */
    public static User create(String name, Email email, PasswordHash passwordHash) {
        Objects.requireNonNull(email);
        Objects.requireNonNull(passwordHash);
        return new User(UserId.newId(), name, email, passwordHash, true);
    }

    /** Pessoa da equipe cadastrada pelo dono: sem e-mail e sem senha (entra por PIN). */
    public static User teamMember(String name) {
        return new User(UserId.newId(), Names.required(name, "Informe o nome da pessoa"), null, null, true);
    }

    public boolean hasPasswordLogin() {
        return email != null;
    }
}
