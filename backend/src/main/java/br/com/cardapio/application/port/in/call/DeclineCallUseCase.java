package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.call.model.CallId;

public interface DeclineCallUseCase {

    /** "Recusar" um pedido de liberação: o celular não está no salão. */
    StaffCall decline(EstablishmentId establishmentId, CallId callId);
}
