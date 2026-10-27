package br.com.cardapio.domain.device.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** O aparelho é uma TV (403). */
public class PinNotAllowedOnDeviceException extends DomainException {

    public PinNotAllowedOnDeviceException() {
        super("Este aparelho é uma TV: nele ninguém entra com PIN.");
    }
}
