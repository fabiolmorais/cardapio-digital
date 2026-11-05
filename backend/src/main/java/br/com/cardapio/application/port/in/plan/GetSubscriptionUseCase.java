package br.com.cardapio.application.port.in.plan;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface GetSubscriptionUseCase {

    SubscriptionOverview get(EstablishmentId establishmentId);
}
