package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.shared.model.Email;

public interface LoadUserPort {

    boolean existsByEmail(Email email);
}
