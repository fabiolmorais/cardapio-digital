package br.com.cardapio.adapter.in.web.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitRuleTest {

    @Test
    void eachEntryPointFallsInItsGroup() {
        assertThat(RateLimitRule.of("POST", "/api/v1/auth/login")).contains(RateLimitRule.LOGIN);
        assertThat(RateLimitRule.of("POST", "/api/v1/auth/password-reset")).contains(RateLimitRule.PASSWORD_RESET);
        assertThat(RateLimitRule.of("POST", "/api/v1/auth/password-reset/confirm"))
                .contains(RateLimitRule.PASSWORD_RESET);
        assertThat(RateLimitRule.of("POST", "/api/v1/auth/pin")).contains(RateLimitRule.PIN);
        assertThat(RateLimitRule.of("POST", "/api/v1/device-pairings")).contains(RateLimitRule.PAIRING);
        assertThat(RateLimitRule.of("POST", "/api/v1/public/tables/abc123/session")).contains(RateLimitRule.TABLE_SESSION);
        assertThat(RateLimitRule.of("POST", "/api/v1/public/tables/abc123/calls")).contains(RateLimitRule.TABLE_ACTIONS);
        assertThat(RateLimitRule.of("DELETE", "/api/v1/public/tables/abc123/calls/9f1c"))
                .contains(RateLimitRule.TABLE_ACTIONS);
        assertThat(RateLimitRule.of("PUT", "/api/v1/public/tables/abc123/rodizio/card"))
                .contains(RateLimitRule.TABLE_ACTIONS);
        assertThat(RateLimitRule.of("DELETE", "/api/v1/public/tables/abc123/order-items/7a2b"))
                .contains(RateLimitRule.TABLE_ACTIONS);
    }

    @Test
    void readingAndTheRestOfTheApiAreNotLimited() {
        // Ler a mesa, o cardápio e os pedidos não conta: a tela atualiza toda hora.
        assertThat(RateLimitRule.of("GET", "/api/v1/public/tables/abc123/calls")).isEmpty();
        assertThat(RateLimitRule.of("GET", "/api/v1/public/tables/abc123")).isEmpty();
        assertThat(RateLimitRule.of("POST", "/api/v1/auth/refresh")).isEmpty();
        assertThat(RateLimitRule.of("POST", "/api/v1/device-pairings/9f1c/claim")).isEmpty(); // exige o dono logado
        assertThat(RateLimitRule.of("POST", "/api/v1/tables")).isEmpty();
    }
}
