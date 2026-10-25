package br.com.cardapio.domain.shared.model;

/**
 * Valor em reais, guardado em centavos para evitar erros de arredondamento.
 */
public record Money(long cents) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Valor não pode ser negativo");
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        return new Money(Math.multiplyExact(cents, quantity));
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(cents, other.cents);
    }
}
