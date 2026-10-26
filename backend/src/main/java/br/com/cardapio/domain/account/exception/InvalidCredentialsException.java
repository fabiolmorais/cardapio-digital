package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/**
 * Login recusado. A mesma mensagem para e-mail inexistente, senha errada ou acesso desativado,
 * para não revelar quais e-mails têm cadastro.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("E-mail ou senha incorretos");
    }
}
