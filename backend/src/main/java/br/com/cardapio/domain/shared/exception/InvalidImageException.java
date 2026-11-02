package br.com.cardapio.domain.shared.exception;

/** 400: o arquivo que chegou não é uma imagem que dá para usar (o tipo real, e não o que o navegador disse). */
public class InvalidImageException extends InvalidValueException {

    public InvalidImageException(String message) {
        super(message);
    }
}
