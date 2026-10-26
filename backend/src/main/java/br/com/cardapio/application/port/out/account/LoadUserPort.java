package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.model.Email;

import java.util.Optional;

/**
 * Usuários são globais (a mesma pessoa pode trabalhar em vários restaurantes),
 * por isso esta consulta não recebe establishmentId.
 */
public interface LoadUserPort {

    boolean existsByEmail(Email email);

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId id);
}
