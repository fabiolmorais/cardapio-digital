package br.com.cardapio.domain.call.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

/**
 * 409: o chamado já não está no estado que a ação pede. O caso comum: dois garçons tocam em
 * "Aceitar" quase juntos, e o segundo recebe esta mensagem em vez de "aceitar de novo".
 */
public class CallStateException extends ConflictException {

    public CallStateException(String message) {
        super(message);
    }
}
