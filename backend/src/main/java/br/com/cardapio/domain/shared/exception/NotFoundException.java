package br.com.cardapio.domain.shared.exception;

/**
 * Algo que não existe para quem pediu (404). Vale também quando existe, mas é de outro
 * restaurante: a resposta é a mesma, para não revelar o que existe fora daqui.
 */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String message) {
        super(message);
    }
}
