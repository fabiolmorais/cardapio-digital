package br.com.cardapio.application.service.presence;

import br.com.cardapio.application.port.in.presence.PresenceSettings;
import br.com.cardapio.application.port.in.presence.UpdatePresenceRuleCommand;
import br.com.cardapio.application.port.in.presence.UpdatePresenceRuleUseCase;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.domain.presence.model.IpRange;
import br.com.cardapio.domain.presence.model.PresenceRule;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class UpdatePresenceRuleService implements UpdatePresenceRuleUseCase {

    private final PresenceRulePort rules;
    private final Clock clock;

    public UpdatePresenceRuleService(PresenceRulePort rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    /** Cada IP é conferido aqui (IpRange.parse): um IP errado vira 400 com o texto que veio. */
    @Override
    @Transactional
    public PresenceSettings update(UpdatePresenceRuleCommand command) {
        Instant now = clock.instant();
        var networks = command.networks().stream().map(IpRange::parse).toList();
        PresenceRule rule = rules.find(command.establishmentId())
                .orElseGet(() -> PresenceRule.empty(command.establishmentId(), now))
                .update(networks, command.wifiName(), command.location(), now);
        rules.save(rule);
        return PresenceSettings.of(rule, command.clientIp());
    }
}
