package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.CallId;

public interface FinishCallUseCase {

    StaffCall finish(EstablishmentId establishmentId, CallId callId);
}
