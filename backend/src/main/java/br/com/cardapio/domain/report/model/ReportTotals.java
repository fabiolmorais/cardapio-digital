package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.model.Money;

import java.util.Objects;

/**
 * As comandas fechadas de um período: quanto faturaram e quantas foram.
 *
 * @param revenue soma das comandas fechadas, sem a taxa de serviço (ADR 0013)
 * @param tabs    quantas comandas fecharam
 */
public record ReportTotals(Money revenue, int tabs) {

    public static final ReportTotals EMPTY = new ReportTotals(Money.ZERO, 0);

    public ReportTotals {
        Objects.requireNonNull(revenue, "faturamento é obrigatório");
        if (tabs < 0) {
            throw new IllegalArgumentException("comandas não pode ser negativo");
        }
    }

    /** O tíquete médio por comanda, arredondado ao centavo; zero sem comandas. */
    public Money averageTicket() {
        return tabs == 0 ? Money.ZERO : Money.ofCents(Math.round((double) revenue.cents() / tabs));
    }
}
