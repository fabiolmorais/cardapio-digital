package br.com.cardapio.domain.plan.service;

import br.com.cardapio.domain.plan.exception.FeatureNotInPlanException;
import br.com.cardapio.domain.plan.exception.PlanLimitReachedException;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.plan.model.PlanResource;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.shared.model.Limit;

/**
 * O que o plano deixa fazer. Atingir o limite bloqueia só o próximo cadastro: o que já existe
 * continua ativo (especificação, "Regras de negócio"), inclusive depois de mudar para um plano menor.
 */
public final class PlanAllowance {

    private PlanAllowance() {
    }

    /** Cabe mais um (mesa, pessoa ou setor) no plano? Senão, PlanLimitReachedException. */
    public static void checkRoomForOneMore(Plan plan, PlanUsage usage, PlanResource resource) {
        Limit limit = resource.limitIn(plan);
        if (!limit.allows(Math.toIntExact(usage.of(resource) + 1))) {
            throw new PlanLimitReachedException(plan.name(), resource, limit.max());
        }
    }

    public static void checkVideos(Plan plan) {
        if (!plan.includes(PlanFeature.PRODUCT_VIDEOS)) {
            throw new FeatureNotInPlanException(
                    "Vídeos nos pratos fazem parte dos planos Pro, Business e Enterprise. Mude de plano para enviar.");
        }
    }
}
