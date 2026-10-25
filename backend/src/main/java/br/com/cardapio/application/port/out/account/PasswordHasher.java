package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
