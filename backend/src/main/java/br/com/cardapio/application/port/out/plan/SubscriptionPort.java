package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.plan.model.PlanUsage;
import br.com.cardapio.domain.plan.model.Subscription;

import java.util.Optional;

/**
 * A assinatura fica na empresa (organization), mas toda busca parte do restaurante do usuário:
 * quem pede nunca escolhe de qual empresa é a assinatura.
 */
public interface SubscriptionPort {

    /** Grava a assinatura nova (no cadastro) ou as mudanças dela. */
    void save(Subscription subscription);

    /** A assinatura da empresa deste restaurante, com o plano. */
    Optional<CurrentSubscription> find(EstablishmentId establishmentId);

    /**
     * O mesmo que {@link #find}, travando a assinatura até o fim da transação: dois cadastros ao
     * mesmo tempo esperam um pelo outro, e não passam juntos da última vaga do plano.
     */
    Optional<CurrentSubscription> lock(EstablishmentId establishmentId);

    /** Mesas, pessoas da equipe e setores ativos do restaurante. */
    PlanUsage usage(EstablishmentId establishmentId);
}
