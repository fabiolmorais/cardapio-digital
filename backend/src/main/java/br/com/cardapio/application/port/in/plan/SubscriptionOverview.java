package br.com.cardapio.application.port.in.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.Standing;

import java.time.Instant;

/**
 * A assinatura vista pelo dono: o plano, a situação de agora, as datas e quanto já usa dos limites.
 *
 * @param coveredUntil o fim do teste grátis ou do período pago
 * @param graceEndsAt  até quando a tolerância vai, depois de vencer
 * @param request      o plano pedido, à espera do pagamento (null se não pediu)
 */
public record SubscriptionOverview(Plan plan, Standing standing, Instant coveredUntil, Instant graceEndsAt,
                                   PlanUsage usage, RequestedPlan request) {
}
