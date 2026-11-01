package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/** 400: o arquivo que chegou não é uma foto que dá para usar (o tipo real, e não o que o navegador disse). */
public class InvalidImageException extends InvalidValueException {

    public InvalidImageException(String message) {
        super(message);
    }
}
