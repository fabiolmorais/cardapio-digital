package br.com.cardapio.application.port.in.call;

import br.com.cardapio.domain.call.model.CallId;

public interface CancelCallUseCase {

    void cancel(String tableToken, String sessionToken, CallId callId);
}
