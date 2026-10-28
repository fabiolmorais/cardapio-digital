package br.com.cardapio.domain.account.exception;

import br.com.cardapio.domain.shared.exception.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException() {
        super("Este e-mail já tem cadastro. Entre com a sua senha ou recupere o acesso.");
    }
}
