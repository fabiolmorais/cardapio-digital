package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.model.Email;

import java.util.List;

/**
 * email é vazio para quem entra só por PIN. establishments: todas as unidades em que a pessoa
 * trabalha, por nome (o seletor de unidade), com a de agora entre elas.
 */
public record CurrentAccount(UserId userId, String name, Email email, EstablishmentAccess establishment,
                             List<EstablishmentAccess> establishments) {

    public CurrentAccount {
        establishments = List.copyOf(establishments);
    }
}
