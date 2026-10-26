package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.shared.model.Email;

public record CurrentAccount(UserId userId, String name, Email email, EstablishmentAccess establishment) {
}
