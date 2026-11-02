package br.com.cardapio.domain.device.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Pediu o Modo TV sem login num aparelho que não é TV (403). */
public class TvModeNotAllowedException extends DomainException {

    public TvModeNotAllowedException() {
        super("O Modo TV sem login é só para aparelhos vinculados como TV. Neste, entre com o PIN.");
    }
}
