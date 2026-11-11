package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.call.model.CallId;

/**
 * "Liberar": o garçom vai até a mesa e digita o código que aparece no celular de quem pediu a
 * liberação. Só aquele celular fica liberado.
 */
public interface ReleaseCallUseCase {

    StaffCall release(EstablishmentId establishmentId, CallId callId, UserId by, String code);
}
