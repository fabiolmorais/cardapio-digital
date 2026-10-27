package br.com.cardapio.domain.device.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Sem token de aparelho, token desconhecido ou aparelho desvinculado (401). */
public class InvalidDeviceException extends DomainException {

    public InvalidDeviceException() {
        super("Este aparelho não está vinculado ao restaurante. Peça ao gerente para vincular.");
    }
}
