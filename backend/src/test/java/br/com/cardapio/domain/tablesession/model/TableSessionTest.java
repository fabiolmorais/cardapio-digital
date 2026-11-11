package br.com.cardapio.domain.tablesession.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableSessionTest {

    private static final Instant NOW = Instant.parse("2026-11-02T20:00:00Z");
    private final DiningTableId mesa04 = DiningTableId.newId();
    private final TableSession session = TableSession.open(EstablishmentId.newId(), mesa04, TokenHash.of("abc"), NOW);

    @Test
    void itVanishesAfterTwoHoursWithoutUse() {
        assertThat(session.isActive(NOW.plus(Duration.ofMinutes(119)))).isTrue();
        assertThat(session.isActive(NOW.plus(TableSession.IDLE_LIMIT))).isFalse();
    }

    @Test
    void eachUsePushesTheDeadline() {
        Instant later = NOW.plus(Duration.ofMinutes(90));

        TableSession touched = session.touch(later);

        assertThat(touched.isActive(NOW.plus(Duration.ofMinutes(150)))).isTrue();
        assertThat(touched.lastSeenAt()).isEqualTo(later);
        assertThat(touched.createdAt()).isEqualTo(NOW);
        assertThat(touched.id()).isEqualTo(session.id());
    }

    @Test
    void itBelongsToOneTable() {
        assertThat(session.isFor(mesa04)).isTrue();
        assertThat(session.isFor(DiningTableId.newId())).isFalse();
    }

    @Test
    void theHashIsSha256AndNeverTheToken() {
        TokenHash hash = TokenHash.of("token-secreto");

        assertThat(hash.value()).hasSize(64).doesNotContain("token-secreto");
        assertThat(TokenHash.of("token-secreto")).isEqualTo(hash);
        assertThat(TokenHash.of("outro")).isNotEqualTo(hash);
    }

    @Test
    void itStartsOutsideAndTheFirstProofStays() {
        assertThat(session.isPresent()).isFalse();

        TableSession byWifi = session.confirmPresence(PresenceMethod.WIFI, NOW);
        TableSession again = byWifi.confirmPresence(PresenceMethod.GPS, NOW.plusSeconds(60));

        assertThat(byWifi.isPresent()).isTrue();
        assertThat(again.presenceMethod()).isEqualTo(PresenceMethod.WIFI);
        assertThat(again.presenceConfirmedAt()).isEqualTo(NOW);
        assertThat(byWifi.touch(NOW.plusSeconds(90)).presenceMethod()).isEqualTo(PresenceMethod.WIFI);
    }

    @Test
    void theWaiterUnlockRecordsWho() {
        UserId carlos = UserId.newId();

        TableSession unlocked = session.unlock(carlos, NOW);

        assertThat(unlocked.presenceMethod()).isEqualTo(PresenceMethod.STAFF);
        assertThat(unlocked.unlockedBy()).isEqualTo(carlos);
        assertThatThrownBy(() -> session.confirmPresence(PresenceMethod.STAFF, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theIdleLimitComesFromTheRestaurantAndStays() {
        TableSession short30 = TableSession.open(EstablishmentId.newId(), mesa04, TokenHash.of("x"),
                Duration.ofMinutes(30), NOW);

        assertThat(short30.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(short30.touch(NOW.plusSeconds(600)).expiresAt()).isEqualTo(NOW.plusSeconds(600 + 1800));
    }

    @Test
    void aBlockedPhoneCannotProveOrBeUnlocked() {
        UserId carlos = UserId.newId();
        TableSession blocked = session.block(carlos, NOW);

        assertThat(blocked.isBlocked()).isTrue();
        assertThat(blocked.blockedBy()).isEqualTo(carlos);
        assertThat(blocked.confirmPresence(PresenceMethod.WIFI, NOW).isPresent()).isFalse();
        assertThat(blocked.unlock(carlos, NOW).isPresent()).isFalse();
        assertThat(blocked.block(UserId.newId(), NOW.plusSeconds(5)).blockedAt()).isEqualTo(NOW);
    }

    @Test
    void unblockingKeepsThePresenceItHadBefore() {
        UserId carlos = UserId.newId();
        TableSession present = session.confirmPresence(PresenceMethod.GPS, NOW);

        TableSession unblocked = present.block(carlos, NOW).unblock();

        assertThat(unblocked.isBlocked()).isFalse();
        assertThat(unblocked.blockedBy()).isNull();
        assertThat(unblocked.presenceMethod()).isEqualTo(PresenceMethod.GPS);
        assertThat(session.unblock()).isSameAs(session); // sem bloqueio, nada muda
    }
}
