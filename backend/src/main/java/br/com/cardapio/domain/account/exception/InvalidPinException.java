package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** PIN errado (401). Diz quantas tentativas sobram antes do bloqueio. */
public class InvalidPinException extends DomainException {

    private final int remainingAttempts;

    public InvalidPinException(int remainingAttempts) {
        super(remainingAttempts == 1
                ? "PIN incorreto. Resta 1 tentativa."
                : "PIN incorreto. Restam " + remainingAttempts + " tentativas.");
        this.remainingAttempts = remainingAttempts;
    }

    public int remainingAttempts() {
        return remainingAttempts;
    }
}
