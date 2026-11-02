package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallId;

public interface BlockCallerUseCase {

    /**
     * "Bloquear este celular", a partir de um chamado da fila: o celular que abriu o chamado não
     * chama mais nesta visita, e os chamados abertos dele são cancelados.
     */
    void block(EstablishmentId establishmentId, CallId callId, UserId by);
}
