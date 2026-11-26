package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * 422: o que vale para a mesa não vale para o balcão (ADR 0015): QR Code, pedido pela mesa sem
 * escolher a comanda, rodízio.
 */
public class CounterNotAllowedException extends DomainException {

    public CounterNotAllowedException(String message) {
        super(message);
    }
}
