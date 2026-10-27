package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Erros demais: o PIN desta pessoa está bloqueado por um tempo (429). */
public class PinLockedException extends DomainException {

    private final long minutesLeft;

    public PinLockedException(long minutesLeft) {
        super("Muitas tentativas erradas. Tente de novo em " + minutesLeft
                + (minutesLeft == 1 ? " minuto" : " minutos") + " ou peça um PIN novo ao gerente.");
        this.minutesLeft = minutesLeft;
    }

    public long minutesLeft() {
        return minutesLeft;
    }
}
