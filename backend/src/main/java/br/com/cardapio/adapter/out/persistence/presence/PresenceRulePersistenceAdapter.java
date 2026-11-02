package br.com.cardapio.adapter.out.persistence.presence;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.presence.model.GeoFence;
import br.com.cardapio.domain.presence.model.IpRange;
import br.com.cardapio.domain.presence.model.PresenceRule;
import br.com.cardapio.domain.presence.model.PresenceRuleId;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
class PresenceRulePersistenceAdapter implements PresenceRulePort {

    private final PresenceRuleJpaRepository rules;
    private final EstablishmentFilter establishmentFilter;

    PresenceRulePersistenceAdapter(PresenceRuleJpaRepository rules, EstablishmentFilter establishmentFilter) {
        this.rules = rules;
        this.establishmentFilter = establishmentFilter;
    }

    @Override
    public Optional<PresenceRule> find(EstablishmentId establishmentId) {
        establishmentFilter.enable(establishmentId);
        return rules.findByEstablishmentId(establishmentId.value()).map(e -> new PresenceRule(
                new PresenceRuleId(e.getId()), new EstablishmentId(e.getEstablishmentId()),
                Arrays.stream(e.getNetworks()).map(IpRange::parse).toList(), e.getWifiName(),
                e.getLatitude() == null ? null : new GeoFence(e.getLatitude(), e.getLongitude(), e.getRadiusMeters()),
                Duration.ofMinutes(e.getSessionIdleMinutes()), e.getUpdatedAt()));
    }

    @Override
    public void save(PresenceRule r) {
        establishmentFilter.enable(r.establishmentId());
        GeoFence location = r.location();
        rules.save(new PresenceRuleJpaEntity(r.id().value(), r.establishmentId().value(),
                r.networks().stream().map(IpRange::toString).toArray(String[]::new), r.wifiName(),
                location == null ? null : location.latitude(), location == null ? null : location.longitude(),
                location == null ? null : location.radiusMeters(), (int) r.sessionIdleLimit().toMinutes(),
                r.updatedAt()));
    }
}
