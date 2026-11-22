package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.Optional;

/**
 * O tíquete por pessoa (REL-04): o faturamento das comandas e quantas pessoas estavam nelas.
 *
 * @param people as pessoas do rodízio, ou as que o garçom informou fora dele
 */
public record PerPersonTicket(Money revenue, int people) {

    public static final PerPersonTicket EMPTY = new PerPersonTicket(Money.ZERO, 0);

    /** O tíquete por pessoa, arredondado ao centavo; vazio sem ninguém contado. */
    public Optional<Money> average() {
        return people == 0 ? Optional.empty() : Optional.of(Money.ofCents(Math.round((double) revenue.cents() / people)));
    }
}
