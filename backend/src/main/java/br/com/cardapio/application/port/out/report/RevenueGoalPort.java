package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.RevenueGoal;
import br.com.cardapio.domain.shared.model.Money;

import java.time.YearMonth;
import java.util.Optional;

/** As metas de faturamento do restaurante, uma por mês (REL-79). */
public interface RevenueGoalPort {

    Optional<RevenueGoal> find(EstablishmentId establishmentId, YearMonth month);

    /** Grava a meta do mês; vazio tira a meta. */
    void save(EstablishmentId establishmentId, YearMonth month, Optional<Money> amount);
}
