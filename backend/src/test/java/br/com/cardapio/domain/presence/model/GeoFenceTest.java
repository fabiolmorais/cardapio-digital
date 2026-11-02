package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoFenceTest {

    /** Praça da Liberdade, São Paulo. */
    private final GeoFence sakura = new GeoFence(-23.5555, -46.6355, 100);

    @Test
    void measuresTheDistanceInMeters() {
        // 0,001 grau de latitude são uns 111 metros em qualquer lugar.
        assertThat(sakura.distanceTo(-23.5565, -46.6355)).isCloseTo(111.2, within(0.5));
    }

    @Test
    void admitsWhoIsInsideWithGoodAccuracy() {
        assertThat(sakura.admits(new GeoPosition(-23.5560, -46.6355, 20))).isTrue(); // ~56 m
        assertThat(sakura.admits(new GeoPosition(-23.5570, -46.6355, 20))).isFalse(); // ~167 m
    }

    @Test
    void theAccuracyGivesSomeSlackAtTheBorder() {
        // ~111 m do ponto, com raio de 100: fora por pouco, mas a precisão de 30 m cobre.
        assertThat(sakura.admits(new GeoPosition(-23.5565, -46.6355, 30))).isTrue();
        assertThat(sakura.admits(new GeoPosition(-23.5565, -46.6355, 5))).isFalse();
    }

    @Test
    void aVaguePositionDoesNotCount() {
        assertThat(sakura.admits(new GeoPosition(-23.5555, -46.6355, GeoFence.MAX_ACCURACY_METERS + 1))).isFalse();
    }

    @Test
    void radiusAndCoordinatesHaveLimits() {
        assertThatThrownBy(() -> new GeoFence(-23.5, -46.6, 10)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new GeoFence(-23.5, -46.6, 5000)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new GeoFence(-95, -46.6, 100)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new GeoPosition(Double.NaN, -46.6, 10)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new GeoPosition(-23.5, -46.6, -1)).isInstanceOf(InvalidValueException.class);
    }
}
