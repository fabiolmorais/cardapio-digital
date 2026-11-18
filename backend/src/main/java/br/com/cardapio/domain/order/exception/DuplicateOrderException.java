package br.com.cardapio.domain.order.exception;

import br.com.cardapio.domain.order.model.AlreadyOrdered;
import br.com.cardapio.domain.shared.exception.ConflictException;

import java.util.List;

/** 409: a mesa já pediu estes produtos. O garçom tira do pedido ou confirma que é outro pedido. */
public class DuplicateOrderException extends ConflictException {

    private final transient List<AlreadyOrdered> duplicates;

    public DuplicateOrderException(List<AlreadyOrdered> duplicates) {
        super("Esta mesa já pediu estes itens. Confira antes de enviar.");
        this.duplicates = List.copyOf(duplicates);
    }

    public List<AlreadyOrdered> duplicates() {
        return duplicates;
    }
}
