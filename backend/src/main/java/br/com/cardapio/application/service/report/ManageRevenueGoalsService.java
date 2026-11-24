package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.ManageRevenueGoalsUseCase;
import br.com.cardapio.application.port.in.report.RevenueGoals;
import br.com.cardapio.application.port.out.account.EstablishmentTimeZonePort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.report.RevenueGoalPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.RevenueGoal;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

/** A meta de faturamento é do Business: os outros planos recebem 403. */
public class ManageRevenueGoalsService implements ManageRevenueGoalsUseCase {

    private final SubscriptionPort subscriptions;
    private final EstablishmentTimeZonePort timeZones;
    private final RevenueGoalPort goals;
    private final Clock clock;

    public ManageRevenueGoalsService(SubscriptionPort subscriptions, EstablishmentTimeZonePort timeZones,
                                     RevenueGoalPort goals, Clock clock) {
        this.subscriptions = subscriptions;
        this.timeZones = timeZones;
        this.goals = goals;
        this.clock = clock;
    }

    private YearMonth thisMonth(EstablishmentId id) {
        return YearMonth.now(clock.withZone(timeZones.timeZoneOf(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueGoals get(EstablishmentId establishmentId) {
        ReportAccesses.of(subscriptions, establishmentId).requireFull();
        return view(establishmentId, thisMonth(establishmentId));
    }

    @Override
    @Transactional
    public RevenueGoals update(EstablishmentId establishmentId, List<GoalChange> changes) {
        ReportAccesses.of(subscriptions, establishmentId).requireFull();
        YearMonth current = thisMonth(establishmentId);
        for (GoalChange change : changes) {
            if (!change.month().equals(current) && !change.month().equals(current.plusMonths(1))) {
                throw new InvalidValueException("Só dá para mudar a meta deste mês e a do próximo");
            }
            // Valida o valor pelo próprio domínio (de R$ 0,01 a R$ 100 milhões).
            change.amount().ifPresent(amount -> new RevenueGoal(change.month(), amount));
        }
        changes.forEach(change -> goals.save(establishmentId, change.month(), change.amount()));
        return view(establishmentId, current);
    }

    private RevenueGoals view(EstablishmentId id, YearMonth current) {
        YearMonth next = current.plusMonths(1);
        return new RevenueGoals(current, goals.find(id, current).map(RevenueGoal::amount), next,
                goals.find(id, next).map(RevenueGoal::amount));
    }
}
