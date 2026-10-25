package br.com.cardapio.domain.shared.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitTest {

    @Test
    void allowsUpToTheLimit() {
        Limit limit = Limit.of(20);
        assertThat(limit.allows(20)).isTrue();
        assertThat(limit.allows(21)).isFalse();
    }

    @Test
    void unlimitedAllowsAnything() {
        assertThat(Limit.unlimited().isUnlimited()).isTrue();
        assertThat(Limit.unlimited().allows(1_000_000)).isTrue();
    }

    @Test
    void rejectsNegativeLimit() {
        assertThatThrownBy(() -> Limit.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
