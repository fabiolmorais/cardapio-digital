package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.shared.model.Email;

/**
 * Usuários são globais (a mesma pessoa pode trabalhar em vários restaurantes),
 * por isso esta consulta não recebe establishmentId.
 */
public interface LoadUserPort {

    boolean existsByEmail(Email email);
}
