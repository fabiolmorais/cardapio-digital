package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.out.plan.CurrentSubscription;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.plan.model.PlanResource;
import br.com.cardapio.domain.plan.service.PlanAllowance;

import java.util.Optional;

/**
 * O que os casos de uso de cadastro chamam antes de criar ou religar uma mesa, uma pessoa ou um
 * setor (e antes de enviar um vídeo). É pública porque esses casos de uso moram em outros pacotes.
 */
public final class PlanAllowances {

    private PlanAllowances() {
    }

    /** Trava a assinatura, conta o que está ativo e confere se cabe mais um. */
    public static void requireRoomForOneMore(SubscriptionPort subscriptions, EstablishmentId establishmentId,
                                             PlanResource resource) {
        CurrentSubscription current = require(subscriptions.lock(establishmentId));
        PlanAllowance.checkRoomForOneMore(current.plan(), subscriptions.usage(establishmentId), resource);
    }

    public static void requireVideos(SubscriptionPort subscriptions, EstablishmentId establishmentId) {
        PlanAllowance.checkVideos(require(subscriptions.find(establishmentId)).plan());
    }

    static CurrentSubscription require(Optional<CurrentSubscription> current) {
        // Todo cadastro ganha a assinatura (e a migração V26 deu uma a quem já existia).
        return current.orElseThrow(() -> new IllegalStateException("restaurante sem assinatura"));
    }
}
