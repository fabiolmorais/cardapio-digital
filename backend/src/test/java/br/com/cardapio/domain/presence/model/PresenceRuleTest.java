package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresenceRuleTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final GeoPosition inside = new GeoPosition(-23.5556, -46.6355, 20);

    private PresenceRule rule(List<String> networks, GeoFence location) {
        return PresenceRule.empty(sakura, NOW).update(networks.stream().map(IpRange::parse).toList(), " Sakura ",
                location, NOW);
    }

    @Test
    void withNothingConfiguredOnlyTheWaiterUnlocks() {
        PresenceRule empty = PresenceRule.empty(sakura, NOW);

        assertThat(empty.check("189.45.212.18", inside)).isEmpty();
    }

    @Test
    void theNetworkComesFirstAndAsksNothing() {
        PresenceRule rule = rule(List.of("189.45.212.18"), new GeoFence(-23.5555, -46.6355, 100));

        assertThat(rule.check("189.45.212.18", null)).contains(PresenceMethod.WIFI);
        assertThat(rule.check("200.1.1.1", null)).isEmpty();
        assertThat(rule.check("200.1.1.1", inside)).contains(PresenceMethod.GPS);
    }

    @Test
    void theLocationOnlyCountsIfTheRestaurantMarkedIt() {
        assertThat(rule(List.of("189.45.212.18"), null).check("200.1.1.1", inside)).isEmpty();
    }

    @Test
    void networksAreDeduplicatedAndLimited() {
        PresenceRule rule = rule(List.of("189.45.212.18", "189.45.212.18/32"), null);

        assertThat(rule.networks()).hasSize(1);
        assertThat(rule.wifiName()).isEqualTo("Sakura");
        List<String> eleven = IntStream.rangeClosed(1, 11).mapToObj(i -> "10.0.0." + i).toList();
        assertThatThrownBy(() -> rule(eleven, null)).isInstanceOf(InvalidValueException.class);
    }
}
