package br.com.cardapio.adapter.out.persistence.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.plan.model.PlanId;
import br.com.cardapio.domain.plan.model.SupportLevel;
import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;

import java.util.stream.Collectors;

final class PlanPersistenceMapper {

    private PlanPersistenceMapper() {
    }

    static Plan toDomain(PlanJpaEntity entity) {
        return new Plan(
                new PlanId(entity.getId()),
                entity.getCode(),
                entity.getName(),
                toMoney(entity.getMonthlyPriceCents()),
                toMoney(entity.getAnnualPriceCents()),
                toLimit(entity.getMaxTables()),
                toLimit(entity.getMaxUsers()),
                toLimit(entity.getMaxSectors()),
                entity.getHistoryDays(),
                entity.getEntitlements().stream()
                        .map(PlanFeature::valueOf)
                        .collect(Collectors.toSet()),
                SupportLevel.valueOf(entity.getSupportLevel()),
                entity.getDisplayOrder());
    }

    private static Money toMoney(Long cents) {
        return cents == null ? null : Money.ofCents(cents);
    }

    private static Limit toLimit(Integer max) {
        return max == null ? Limit.unlimited() : Limit.of(max);
    }
}
