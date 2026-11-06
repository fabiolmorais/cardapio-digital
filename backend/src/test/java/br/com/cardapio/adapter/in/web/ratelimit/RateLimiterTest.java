package br.com.cardapio.adapter.in.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    /** Um relógio que o teste adianta. */
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-06T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final RateLimiter limiter = new RateLimiter(clock);
    private final RateLimitProperties.Limit threePerMinute = new RateLimitProperties.Limit(3, Duration.ofMinutes(1));

    @Test
    void theFourthInTheWindowWaitsUntilItEnds() {
        assertThat(limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute)).isEmpty();
        clock.now = clock.now.plusSeconds(10);
        assertThat(limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute)).isEmpty();
        assertThat(limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute)).isEmpty();

        assertThat(limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute)).contains(Duration.ofSeconds(50));

        // A janela acaba: zera.
        clock.now = clock.now.plusSeconds(50);
        assertThat(limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute)).isEmpty();
    }

    @Test
    void eachIpAndEachGroupCountApart() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute);
        }

        assertThat(limiter.tryAcquire("LOGIN|10.0.0.2", threePerMinute)).isEmpty();
        assertThat(limiter.tryAcquire("PIN|10.0.0.1", threePerMinute)).isEmpty();
        assertThat(limiter.tryAcquire("LOGIN|10.0.0.1", threePerMinute)).isPresent();
    }

    @Test
    void ipsThatDidNotComeBackAreSweptAway() {
        for (int i = 0; i <= 10_000; i++) {
            limiter.tryAcquire("TABLE_SESSION|10.1." + i, threePerMinute);
        }
        clock.now = clock.now.plus(Duration.ofMinutes(2));

        limiter.tryAcquire("TABLE_SESSION|10.9.9.9", threePerMinute);

        assertThat(limiter.size()).isEqualTo(1);
    }
}
