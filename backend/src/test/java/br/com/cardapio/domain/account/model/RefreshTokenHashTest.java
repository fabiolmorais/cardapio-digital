package br.com.cardapio.domain.account.model;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenHashTest {

    @Test
    void sameTokenAlwaysGivesTheSameHash() {
        assertThat(RefreshTokenHash.of("abc")).isEqualTo(RefreshTokenHash.of("abc"));
        assertThat(RefreshTokenHash.of("abc")).isNotEqualTo(RefreshTokenHash.of("abd"));
    }

    @Test
    void isTheSha256InHex() {
        // Valor conhecido do SHA-256 de "abc".
        assertThat(RefreshTokenHash.of("abc").value())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}