package br.com.cardapio.application.port.in.presence;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface GetPresenceRuleUseCase {

    PresenceSettings get(EstablishmentId establishmentId, String clientIp);
}
