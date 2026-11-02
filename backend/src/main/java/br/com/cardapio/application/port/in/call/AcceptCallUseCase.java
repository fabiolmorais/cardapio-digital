package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallId;

public interface AcceptCallUseCase {

    StaffCall accept(EstablishmentId establishmentId, CallId callId, UserId by);
}
