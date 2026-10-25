package br.com.cardapio.application.port.out.security;

import br.com.cardapio.domain.account.model.PasswordHash;

/**
 * Protege senhas e PINs. A implementação (BCrypt) é detalhe de infraestrutura.
 */
public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
