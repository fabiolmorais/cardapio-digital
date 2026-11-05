package br.com.cardapio.application.port.in.plan;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.plan.model.BillingCycle;

public record RequestPlanCommand(EstablishmentId establishmentId, String planCode, BillingCycle cycle) {
}
