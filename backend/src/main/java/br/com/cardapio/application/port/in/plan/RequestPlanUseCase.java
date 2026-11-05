package br.com.cardapio.application.port.in.plan;

public interface RequestPlanUseCase {

    /** Registra o plano escolhido pelo dono e devolve a assinatura atualizada. */
    SubscriptionOverview request(RequestPlanCommand command);
}
