package br.com.cardapio.domain.call.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * 422: o garçom digitou um código que não é o que aparece no celular de quem pediu a liberação.
 * O pedido continua aberto: ele confere a tela do cliente e tenta de novo.
 */
public class WrongReleaseCodeException extends DomainException {

    public WrongReleaseCodeException() {
        super("O código não confere. Peça ao cliente para mostrar a tela do celular e digite de novo.");
    }
}
