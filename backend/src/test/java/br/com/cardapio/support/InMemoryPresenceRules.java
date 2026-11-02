package br.com.cardapio.support;

import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.presence.model.PresenceRule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** As regras de presença na memória, para os testes dos serviços. */
public final class InMemoryPresenceRules implements PresenceRulePort {

    public final Map<EstablishmentId, PresenceRule> saved = new HashMap<>();

    @Override
    public Optional<PresenceRule> find(EstablishmentId establishmentId) {
        return Optional.ofNullable(saved.get(establishmentId));
    }

    @Override
    public void save(PresenceRule rule) {
        saved.put(rule.establishmentId(), rule);
    }
}
