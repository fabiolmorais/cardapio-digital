package br.com.cardapio.domain.account.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PinLockoutTest {

    private static final Instant NOW = Instant.parse("2026-10-27T20:00:00Z");
    private final PinLockout clean = PinLockout.clean(MembershipId.newId(), EstablishmentId.newId());

    private PinLockout failTimes(PinLockout lockout, int times, Instant at) {
        for (int i = 0; i < times; i++) {
            lockout = lockout.registerFailure(at);
        }
        return lockout;
    }

    @Test
    void fourErrorsCountDownAndTheFifthLocksForFifteenMinutes() {
        PinLockout four = failTimes(clean, 4, NOW);
        assertThat(four.isLocked(NOW)).isFalse();
        assertThat(four.remainingAttempts()).isEqualTo(1);

        PinLockout five = four.registerFailure(NOW);
        assertThat(five.isLocked(NOW)).isTrue();
        assertThat(five.minutesLeft(NOW)).isEqualTo(15);
        assertThat(five.isLocked(NOW.plus(PinLockout.LOCK_DURATION))).isFalse();
    }

    @Test
    void minutesLeftRoundsUpAndNeverSaysZero() {
        PinLockout locked = failTimes(clean, 5, NOW);

        assertThat(locked.minutesLeft(NOW.plus(Duration.ofMinutes(14)).plusSeconds(1))).isEqualTo(1);
        assertThat(locked.minutesLeft(NOW.plus(Duration.ofMinutes(13)).plusSeconds(30))).isEqualTo(2);
    }

    @Test
    void afterTheLockExpiresTheCountStartsOver() {
        Instant later = NOW.plus(PinLockout.LOCK_DURATION);
        PinLockout again = failTimes(clean, 5, NOW).registerFailure(later);

        assertThat(again.failedAttempts()).isEqualTo(1);
        assertThat(again.isLocked(later)).isFalse();
    }

    @Test
    void resetClearsEverything() {
        PinLockout reset = failTimes(clean, 3, NOW).reset();

        assertThat(reset.hasFailures()).isFalse();
        assertThat(reset.remainingAttempts()).isEqualTo(PinLockout.MAX_ATTEMPTS);
    }
}
