package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.shared.model.Limit;
import br.com.cardapio.domain.shared.model.Money;

final class PlanWebMapper {

    private PlanWebMapper() {
    }

    static PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.id().value(),
                plan.code(),
                plan.name(),
                plan.monthly().map(Money::cents).orElse(null),
                plan.annual().map(Money::cents).orElse(null),
                toNullable(plan.maxTables()),
                toNullable(plan.maxUsers()),
                toNullable(plan.maxSectors()),
                plan.hasUnlimitedHistory() ? null : plan.historyDays(),
                plan.features().stream()
                        .sorted()
                        .map(f -> PlanResponse.PlanFeatureDto.valueOf(f.name()))
                        .toList(),
                PlanResponse.SupportLevelDto.valueOf(plan.support().name()));
    }

    private static Integer toNullable(Limit limit) {
        return limit.isUnlimited() ? null : limit.max();
    }
}
