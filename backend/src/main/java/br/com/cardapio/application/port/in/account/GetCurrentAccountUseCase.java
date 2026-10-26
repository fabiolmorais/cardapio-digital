package br.com.cardapio.application.port.in.account;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;

public interface GetCurrentAccountUseCase {

    CurrentAccount current(UserId userId, EstablishmentId establishmentId);
}
