package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;
import java.util.Optional;

/** Uma mesa no mapa do salão (REL-59): as comandas que fecharam nela e o faturamento delas. */
public record TableUsage(String label, String sector, int tabs, Money revenue) {

    public TableUsage {
        Objects.requireNonNull(label, "mesa é obrigatória");
        Objects.requireNonNull(sector, "setor é obrigatório");
        Objects.requireNonNull(revenue, "faturamento é obrigatório");
    }

    /** O tíquete médio da mesa; vazio sem comandas. */
    public Optional<Money> ticket() {
        return tabs == 0 ? Optional.empty() : Optional.of(Money.ofCents(Math.round((double) revenue.cents() / tabs)));
    }
}
