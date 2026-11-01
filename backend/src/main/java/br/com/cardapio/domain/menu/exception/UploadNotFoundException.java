package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: o arquivo não chegou ao armazenamento (o envio falhou ou a URL venceu). */
public class UploadNotFoundException extends DomainException {

    public UploadNotFoundException() {
        super("A foto não chegou. Envie de novo.");
    }
}
