package br.com.cardapio.application.port.out.presence;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.presence.model.PresenceRule;

import java.util.Optional;

public interface PresenceRulePort {

    /** Vazio se o restaurante nunca configurou: aí vale só a liberação pelo garçom. */
    Optional<PresenceRule> find(EstablishmentId establishmentId);

    void save(PresenceRule rule);
}
