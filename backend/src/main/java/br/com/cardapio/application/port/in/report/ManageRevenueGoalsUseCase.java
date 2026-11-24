package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/** A meta de faturamento na gestão (Business; só o dono e os gerentes). */
public interface ManageRevenueGoalsUseCase {

    /** Uma mudança: a meta do mês (vazio tira a meta). */
    record GoalChange(YearMonth month, Optional<Money> amount) {
    }

    RevenueGoals get(EstablishmentId establishmentId);

    /** Muda a meta deste mês e/ou a do próximo; outros meses são recusados. */
    RevenueGoals update(EstablishmentId establishmentId, List<GoalChange> changes);
}
