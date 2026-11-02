package br.com.cardapio.domain.call.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 429: o mesmo celular abriu chamados demais em pouco tempo (abrir e cancelar sem parar, por exemplo). */
public class TooManyCallsException extends DomainException {

    public TooManyCallsException() {
        super("Muitos chamados em pouco tempo. Espere um minuto e tente de novo.");
    }
}
