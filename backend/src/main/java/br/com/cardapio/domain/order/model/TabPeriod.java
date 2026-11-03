package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Os dias do histórico de comandas, no calendário do restaurante (o "dia 3" de São Paulo, e não
 * o de Greenwich). Os dois dias entram. Até um ano de cada vez: uma consulta maior que isso é
 * relatório (plano Business), e não o histórico da operação.
 */
public record TabPeriod(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 366;

    public TabPeriod {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (to.isBefore(from)) {
            throw new InvalidValueException("A data final vem depois da inicial.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new InvalidValueException("Escolha um período de até um ano.");
        }
    }
}
