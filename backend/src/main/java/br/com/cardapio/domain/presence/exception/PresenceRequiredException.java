package br.com.cardapio.domain.presence.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 403: chamar e pedir a conta exigem a presença confirmada no salão. */
public class PresenceRequiredException extends DomainException {

    public PresenceRequiredException() {
        super("Confirme que você está no restaurante para chamar o garçom.");
    }
}
