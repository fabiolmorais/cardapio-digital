package br.com.cardapio.domain.shared.exception;

/** O pedido bate com algo que já existe (409): um e-mail já cadastrado, um nome repetido. */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String message) {
        super(message);
    }
}
