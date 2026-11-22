package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * Quantas pessoas estão na mesa fora do rodízio (REL-04, o tíquete por pessoa), informado pelo
 * garçom. Opcional.
 */
public record TabPeople(int value) {

    public static final int MAX = 50;

    public TabPeople {
        if (value < 1 || value > MAX) {
            throw new InvalidValueException("Informe de 1 a " + MAX + " pessoas");
        }
    }
}
