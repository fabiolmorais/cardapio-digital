package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.AlertSettings;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-24T23:00:00Z");

    private static Instant ago(int minutes) {
        return NOW.minus(Duration.ofMinutes(minutes));
    }

    @Test
    void theFirstAlertIsForWhatCrossedTheLimitInTheLast15Minutes() {
        assertThat(AlertPolicy.shouldSend(ago(5), Optional.empty(), NOW)).isTrue();
        assertThat(AlertPolicy.shouldSend(ago(40), Optional.empty(), NOW)).isFalse();
    }

    @Test
    void atMostOneEvery15Minutes() {
        assertThat(AlertPolicy.shouldSend(ago(1), Optional.of(ago(10)), NOW)).isFalse();
        assertThat(AlertPolicy.shouldSend(ago(1), Optional.of(ago(15)), NOW)).isTrue();
    }

    @Test
    void whatWasAlreadyWarnedIsNotWarnedAgain() {
        assertThat(AlertPolicy.shouldSend(ago(30), Optional.of(ago(20)), NOW)).isFalse();
        assertThat(AlertPolicy.shouldSend(ago(19), Optional.of(ago(20)), NOW)).isTrue();
    }

    @Test
    void theDefaultsAreKitchenAndCallOn() {
        assertThat(AlertSettings.DEFAULT.kitchenEnabled()).isTrue();
        assertThat(AlertSettings.DEFAULT.callEnabled()).isTrue();
        assertThat(AlertSettings.DEFAULT.tabEnabled()).isFalse();
        assertThat(new AlertSettings(false, 25, false, 3, false, 6).anyEnabled()).isFalse();
    }

    @Test
    void theLimitsHaveARange() {
        assertThatThrownBy(() -> new AlertSettings(true, 4, true, 3, false, 6))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new AlertSettings(true, 25, true, 31, false, 6))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new AlertSettings(true, 25, true, 3, false, 25))
                .isInstanceOf(InvalidValueException.class);
    }
}
