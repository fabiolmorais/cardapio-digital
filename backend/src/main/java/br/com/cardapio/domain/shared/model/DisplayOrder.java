package br.com.cardapio.domain.shared.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.exception.OutdatedListException;

import java.util.*;

/**
 * A ordem nova de uma lista, como a tela manda depois de arrastar: todos os ids, do primeiro
 * ao último. A posição de cada item é o lugar dele nesta lista (0, 1, 2…).
 */
public record DisplayOrder<T>(List<T> ids) {

    public DisplayOrder {
        ids = List.copyOf(Objects.requireNonNull(ids));
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidValueException("A nova ordem tem itens repetidos");
        }
    }

    /**
     * A tela precisa mandar exatamente os itens que existem agora, nem um a mais, nem um a menos.
     * Se algo entrou ou saiu desde que ela carregou, a ordem que ela montou está velha.
     */
    public void requireSameItemsAs(Collection<T> current) {
        if (!Set.copyOf(ids).equals(Set.copyOf(current))) {
            throw new OutdatedListException();
        }
    }

    public int positionOf(T id) {
        int position = ids.indexOf(id);
        if (position < 0) {
            throw new IllegalArgumentException("item fora da ordem: " + id);
        }
        return position;
    }
}
