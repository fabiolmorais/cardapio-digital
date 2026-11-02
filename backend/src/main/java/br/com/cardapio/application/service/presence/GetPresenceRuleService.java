package br.com.cardapio.application.service.presence;

import br.com.cardapio.application.port.in.presence.GetPresenceRuleUseCase;
import br.com.cardapio.application.port.in.presence.PresenceSettings;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.presence.model.PresenceRule;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class GetPresenceRuleService implements GetPresenceRuleUseCase {

    private final PresenceRulePort rules;
    private final Clock clock;

    public GetPresenceRuleService(PresenceRulePort rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PresenceSettings get(EstablishmentId establishmentId, String clientIp) {
        PresenceRule rule = rules.find(establishmentId).orElseGet(() -> PresenceRule.empty(establishmentId,
                clock.instant()));
        return PresenceSettings.of(rule, clientIp);
    }
}
