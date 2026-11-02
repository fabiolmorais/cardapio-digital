package br.com.cardapio.application.service.presence;

import br.com.cardapio.application.port.in.presence.CheckPresenceCommand;
import br.com.cardapio.application.port.in.presence.PresenceSettings;
import br.com.cardapio.application.port.in.presence.PresenceStatus;
import br.com.cardapio.application.port.in.presence.UpdatePresenceRuleCommand;
import br.com.cardapio.application.port.out.presence.PresenceRulePort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTable;
import br.com.cardapio.domain.presence.model.GeoFence;
import br.com.cardapio.domain.presence.model.GeoPosition;
import br.com.cardapio.domain.presence.model.PresenceRule;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.tablesession.exception.TableSessionRequiredException;
import br.com.cardapio.domain.tablesession.model.PresenceMethod;
import br.com.cardapio.support.InMemoryTableSessions;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A conferência da presença na mesa e a configuração na gestão. */
class PresenceServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private static final String QR = "qr-da-mesa-04-00000000000";
    private static final String SALAO = "189.45.212.18";
    private static final String QUATRO_G = "177.10.20.30";

    private final Map<EstablishmentId, PresenceRule> saved = new HashMap<>();
    private final PresenceRulePort rules = new PresenceRulePort() {
        @Override
        public Optional<PresenceRule> find(EstablishmentId establishmentId) {
            return Optional.ofNullable(saved.get(establishmentId));
        }

        @Override
        public void save(PresenceRule rule) {
            saved.put(rule.establishmentId(), rule);
        }
    };
    private final TablesByToken tables = new TablesByToken();
    private final InMemoryTableSessions sessions = new InMemoryTableSessions();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EstablishmentId sakura = EstablishmentId.newId();
    private final GeoPosition inside = new GeoPosition(-23.5556, -46.6355, 20);

    @BeforeEach
    void aTableWithASession() {
        DiningTable mesa = tables.given(sakura, "04", QR);
        sessions.given(sakura, mesa.id(), "celular", NOW);
    }

    private void configure(List<String> networks, GeoFence location) {
        new UpdatePresenceRuleService(rules, clock)
                .update(new UpdatePresenceRuleCommand(sakura, networks, "Sakura Clientes", location, SALAO));
    }

    private PresenceStatus check(String ip, GeoPosition position) {
        return new CheckPresenceService(tables, sessions, rules, clock)
                .check(new CheckPresenceCommand(QR, "celular", ip, position));
    }

    @Test
    void onTheRestaurantWifiThePhoneIsInWithoutAsking() {
        configure(List.of(SALAO), null);

        PresenceStatus status = check(SALAO, null);

        assertThat(status.present()).isTrue();
        assertThat(status.method()).isEqualTo(PresenceMethod.WIFI);
        assertThat(sessions.byToken("celular").presenceMethod()).isEqualTo(PresenceMethod.WIFI);
    }

    @Test
    void outsideTheWifiTheLocationDecides() {
        configure(List.of(SALAO), new GeoFence(-23.5555, -46.6355, 100));

        PresenceStatus first = check(QUATRO_G, null);
        assertThat(first.present()).isFalse();
        assertThat(first.locationEnabled()).isTrue();
        assertThat(first.wifiName()).isEqualTo("Sakura Clientes");

        assertThat(check(QUATRO_G, new GeoPosition(-23.5700, -46.6355, 20)).present()).isFalse(); // ~1,6 km
        assertThat(check(QUATRO_G, inside).method()).isEqualTo(PresenceMethod.GPS);
    }

    @Test
    void withNothingConfiguredOnlyTheWaiterUnlocks() {
        PresenceStatus status = check(SALAO, inside);

        assertThat(status.present()).isFalse();
        assertThat(status.locationEnabled()).isFalse();
        assertThat(sessions.byToken("celular").isPresent()).isFalse();
    }

    @Test
    void withoutTheSessionThereIsNoCheck() {
        assertThatThrownBy(() -> new CheckPresenceService(tables, sessions, rules, clock)
                .check(new CheckPresenceCommand(QR, "inventado", SALAO, null)))
                .isInstanceOf(TableSessionRequiredException.class);
    }

    @Test
    void theSettingsShowTheNetworkOfWhoIsSaving() {
        configure(List.of("189.45.212.18", "2804:14c:6a::/48"), new GeoFence(-23.5555, -46.6355, 150));

        PresenceSettings settings = new GetPresenceRuleService(rules, clock).get(sakura, "2804:14c:6a:9::abcd");

        assertThat(settings.networks()).containsExactly("189.45.212.18/32", "2804:14c:6a:0:0:0:0:0/48");
        assertThat(settings.location().radiusMeters()).isEqualTo(150);
        assertThat(settings.currentNetwork()).isEqualTo("2804:14c:6a:9:0:0:0:0/64");
    }

    @Test
    void aWrongIpIsRefusedWithItsText() {
        assertThatThrownBy(() -> configure(List.of("189.45.212.18", "restaurante.com"), null))
                .isInstanceOf(InvalidValueException.class).hasMessageContaining("restaurante.com");
        assertThat(saved).isEmpty();
    }
}
