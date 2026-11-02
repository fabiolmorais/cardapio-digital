package br.com.cardapio.application.service.tablesession;

import br.com.cardapio.application.port.in.tablesession.TableSessionStarted;
import br.com.cardapio.application.port.out.tablesession.TableSessionPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.exception.TableNotFoundException;
import br.com.cardapio.domain.shared.model.TokenHash;
import br.com.cardapio.domain.tablesession.model.TableSession;
import br.com.cardapio.support.TablesByToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartTableSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");

    private final Map<TokenHash, TableSession> saved = new HashMap<>();
    private final TableSessionPort sessions = new TableSessionPort() {
        @Override
        public void save(TableSession session) {
            saved.put(session.tokenHash(), session);
        }

        @Override
        public Optional<TableSession> findByTokenHash(TokenHash tokenHash) {
            return Optional.ofNullable(saved.get(tokenHash));
        }
    };
    private final TablesByToken tables = new TablesByToken();
    private int generated;
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private StartTableSessionService service() {
        return new StartTableSessionService(tables, sessions, () -> "token-" + (++generated), clock);
    }

    private final EstablishmentId sakura = EstablishmentId.newId();

    @Test
    void theFirstVisitOpensASessionAndOnlyTheHashIsSaved() {
        tables.given(sakura, "04", "qr-da-mesa-04-00000000000");

        TableSessionStarted started = service().start("qr-da-mesa-04-00000000000", null);

        assertThat(started.resumed()).isFalse();
        assertThat(started.token()).isEqualTo("token-1");
        assertThat(started.expiresAt()).isEqualTo(NOW.plus(TableSession.IDLE_LIMIT));
        assertThat(saved).containsOnlyKeys(TokenHash.of("token-1"));
    }

    @Test
    void comingBackWithTheCookieResumesAndPushesTheDeadline() {
        tables.given(sakura, "04", "qr-da-mesa-04-00000000000");
        String token = service().start("qr-da-mesa-04-00000000000", null).token();
        clock = Clock.offset(clock, Duration.ofMinutes(30));

        TableSessionStarted again = service().start("qr-da-mesa-04-00000000000", token);

        assertThat(again.resumed()).isTrue();
        assertThat(again.token()).isEqualTo(token);
        assertThat(again.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)).plus(TableSession.IDLE_LIMIT));
        assertThat(saved).hasSize(1);
    }

    @Test
    void anExpiredCookieACookieFromAnotherTableOrAnInventedOneGetANewSession() {
        tables.given(sakura, "04", "qr-da-mesa-04-00000000000");
        tables.given(sakura, "05", "qr-da-mesa-05-00000000000");
        String token = service().start("qr-da-mesa-04-00000000000", null).token();

        assertThat(service().start("qr-da-mesa-05-00000000000", token).resumed()).isFalse();
        assertThat(service().start("qr-da-mesa-04-00000000000", "inventado").resumed()).isFalse();
        clock = Clock.offset(clock, TableSession.IDLE_LIMIT);
        assertThat(service().start("qr-da-mesa-04-00000000000", token).resumed()).isFalse();
    }

    @Test
    void anUnknownQrIsNotFound() {
        assertThatThrownBy(() -> service().start("qr-que-ninguem-gerou-0000", null))
                .isInstanceOf(TableNotFoundException.class);
        assertThat(saved).isEmpty();
    }
}
