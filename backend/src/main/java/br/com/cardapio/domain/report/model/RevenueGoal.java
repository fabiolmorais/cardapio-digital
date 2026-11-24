package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Money;

import java.time.YearMonth;
import java.util.Objects;

/** A meta de faturamento de um mês (REL-79). */
public record RevenueGoal(YearMonth month, Money amount) {

    /** R$ 100 milhões: mais que isso é erro de digitação. */
    public static final long MAX_CENTS = 10_000_000_000L;

    public RevenueGoal {
        Objects.requireNonNull(month, "mês é obrigatório");
        Objects.requireNonNull(amount, "valor é obrigatório");
        if (amount.isZero() || amount.cents() > MAX_CENTS) {
            throw new InvalidValueException("A meta vai de R$ 0,01 a R$ 100 milhões");
        }
    }
}
