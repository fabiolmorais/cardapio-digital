package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.shared.model.Money;

import java.time.YearMonth;
import java.util.Optional;

/** As metas que dá para mudar na gestão: a deste mês e a do próximo (no fuso do restaurante). */
public record RevenueGoals(YearMonth current, Optional<Money> currentAmount, YearMonth next,
                           Optional<Money> nextAmount) {
}
